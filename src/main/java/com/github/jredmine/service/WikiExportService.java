package com.github.jredmine.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.github.jredmine.entity.Wiki;
import com.github.jredmine.entity.WikiContent;
import com.github.jredmine.entity.WikiPage;
import com.github.jredmine.enums.ResultCode;
import com.github.jredmine.exception.BusinessException;
import com.github.jredmine.mapper.wiki.WikiContentMapper;
import com.github.jredmine.mapper.wiki.WikiPageMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Wiki 导出服务：单页/全部导出为 Markdown 或 HTML
 *
 * @author panfeng
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WikiExportService {

    private final WikiService wikiService;
    private final WikiPageMapper wikiPageMapper;
    private final WikiContentMapper wikiContentMapper;

    /**
     * 导出单页为 Markdown（正文原文，通常已是 Markdown）
     */
    public byte[] exportPageToMarkdown(Long projectId, String titleOrId) {
        WikiPage page = wikiService.getPageByProjectAndTitleOrId(projectId, titleOrId);
        WikiContent content = getLatestContent(page.getId());
        String text = content != null && content.getText() != null ? content.getText() : "";
        String md = "# " + page.getTitle() + "\n\n" + text;
        return md.getBytes(StandardCharsets.UTF_8);
    }

    /**
     * 导出单页为 HTML（简单包装：标题 + pre 包裹正文）
     */
    public byte[] exportPageToHtml(Long projectId, String titleOrId) {
        WikiPage page = wikiService.getPageByProjectAndTitleOrId(projectId, titleOrId);
        WikiContent content = getLatestContent(page.getId());
        String text = content != null && content.getText() != null ? content.getText() : "";
        String escaped = escapeHtml(text);
        String html = "<!DOCTYPE html><html><head><meta charset=\"UTF-8\"><title>" + escapeHtml(page.getTitle()) + "</title></head><body><h1>" + escapeHtml(page.getTitle()) + "</h1><pre>" + escaped + "</pre></body></html>";
        return html.getBytes(StandardCharsets.UTF_8);
    }

    /**
     * 导出全部页面为 ZIP（内为多份 .md 文件）
     */
    public byte[] exportAllToMarkdownZip(Long projectId) {
        Wiki wiki = wikiService.getOrCreateWiki(projectId);
        LambdaQueryWrapper<WikiPage> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WikiPage::getWikiId, wiki.getId()).orderByAsc(WikiPage::getTitle);
        List<WikiPage> pages = wikiPageMapper.selectList(wrapper);
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             ZipOutputStream zos = new ZipOutputStream(baos, StandardCharsets.UTF_8)) {
            for (WikiPage page : pages) {
                WikiContent content = getLatestContent(page.getId());
                String text = content != null && content.getText() != null ? content.getText() : "";
                String md = "# " + page.getTitle() + "\n\n" + text;
                String safeName = sanitizeFileName(page.getTitle()) + ".md";
                zos.putNextEntry(new ZipEntry(safeName));
                zos.write(md.getBytes(StandardCharsets.UTF_8));
                zos.closeEntry();
            }
            zos.finish();
            log.info("Wiki 全部导出 ZIP: projectId={}, 页面数={}", projectId, pages.size());
            return baos.toByteArray();
        } catch (Exception e) {
            log.error("Wiki 导出 ZIP 失败", e);
            throw new BusinessException(ResultCode.SYSTEM_ERROR, "导出失败: " + e.getMessage());
        }
    }

    private WikiContent getLatestContent(Long pageId) {
        LambdaQueryWrapper<WikiContent> w = new LambdaQueryWrapper<>();
        w.eq(WikiContent::getPageId, pageId).orderByDesc(WikiContent::getVersion).last("LIMIT 1");
        return wikiContentMapper.selectOne(w);
    }

    private static String escapeHtml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }

    private static String sanitizeFileName(String title) {
        if (title == null) return "untitled";
        return title.replaceAll("[\\\\/:*?\"<>|]", "_").trim();
    }
}

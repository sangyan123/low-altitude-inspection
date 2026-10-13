package com.drone.platform.module.report.pdf;

import com.drone.platform.module.report.vo.DepartmentReportVO;
import com.drone.platform.module.report.vo.ReportOverviewVO;
import com.drone.platform.module.report.vo.ReportVO;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.util.List;

/** 统计报表 PDF 生成（OpenPDF）。中文优先打包字体，fallback 系统字体。 */
public final class ReportPdfWriter {

    private ReportPdfWriter() {
    }

    public static void write(ReportVO report, OutputStream out) throws IOException {
        Document doc = new Document(PageSize.A4.rotate(), 36, 36, 40, 40);
        try {
            PdfWriter.getInstance(doc, out);
            doc.open();

            BaseFont baseFont = resolveBaseFont();
            Font titleFont = new Font(baseFont, 20, Font.BOLD);
            Font headerFont = new Font(baseFont, 10, Font.BOLD, Color.WHITE);
            Font cellFont = new Font(baseFont, 10);

            Paragraph title = new Paragraph("低空巡检统计报表", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(6);
            doc.add(title);

            Paragraph period = new Paragraph(
                    "报表周期：" + safe(report.getStartDate()) + " 至 " + safe(report.getEndDate())
                            + "    统计粒度：" + granularityName(report.getGranularity()),
                    new Font(baseFont, 10));
            period.setAlignment(Element.ALIGN_CENTER);
            period.setSpacingAfter(14);
            doc.add(period);

            addSection(doc, "一、统计概览", baseFont);
            doc.add(overviewTable(report.getOverview(), baseFont, headerFont, cellFont));

            addSection(doc, "二、部门明细", baseFont);
            doc.add(departmentTable(report.getDepartments(), baseFont, headerFont, cellFont));

            Paragraph footer = new Paragraph("生成时间：" + java.time.LocalDateTime.now()
                    .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                    new Font(baseFont, 8, Font.ITALIC, Color.GRAY));
            footer.setAlignment(Element.ALIGN_RIGHT);
            footer.setSpacingBefore(12);
            doc.add(footer);
        } finally {
            if (doc != null && doc.isOpen()) {
                doc.close();
            }
        }
    }

    private static void addSection(Document doc, String text, BaseFont baseFont) throws IOException {
        Paragraph p = new Paragraph(text, new Font(baseFont, 13, Font.BOLD));
        p.setSpacingBefore(10);
        p.setSpacingAfter(6);
        doc.add(p);
    }

    private static PdfPTable overviewTable(ReportOverviewVO o, BaseFont baseFont, Font headerFont, Font cellFont) throws IOException {
        PdfPTable table = new PdfPTable(new float[]{3, 2, 3, 2, 3, 2});
        table.setWidthPercentage(100);
        addHeader(table, headerFont, "指标", "数值", "指标", "数值", "指标", "数值");
        addRow(table, cellFont,
                "设备总数", str(o.getDeviceTotal()),
                "在线设备", str(o.getDeviceOnline()),
                "飞行中", str(o.getDeviceFlying()));
        addRow(table, cellFont,
                "巡检任务", str(o.getPatrolTotal()),
                "巡检完成", str(o.getPatrolCompleted()) + "（" + rate(o.getPatrolCompletionRate()) + "）",
                "运输任务", str(o.getTransportTotal()));
        addRow(table, cellFont,
                "运输签收", str(o.getTransportSigned()) + "（" + rate(o.getTransportSignedRate()) + "）",
                "飞行计划", str(o.getPlanTotal()),
                "审批通过", str(o.getPlanApproved()) + "（" + rate(o.getPlanApprovalRate()) + "）");
        addRow(table, cellFont,
                "AI识别", str(o.getAiRecognitionTotal()),
                "事件总数", str(o.getEventTotal()),
                "事件销号", str(o.getEventClosed()) + "（" + rate(o.getEventClosureRate()) + "）");
        addRow(table, cellFont,
                "应急事件", str(o.getEmergencyTotal()),
                "进行中应急", str(o.getEmergencyOngoing()),
                "成果文件", str(o.getDataResultTotal()));
        addRow(table, cellFont,
                "影像/视频", str(o.getDataMediaTotal()),
                "三维模型", str(o.getDataModelTotal()),
                "", "");
        return table;
    }

    private static PdfPTable departmentTable(List<DepartmentReportVO> departments, BaseFont baseFont, Font headerFont, Font cellFont) throws IOException {
        PdfPTable table = new PdfPTable(8);
        table.setWidthPercentage(100);
        addHeader(table, headerFont, "部门", "设备", "巡检", "运输", "计划", "事件", "成果", "业务合计");
        if (departments == null || departments.isEmpty()) {
            PdfPCell empty = new PdfPCell(new Phrase("周期内无部门数据", cellFont));
            empty.setColspan(8);
            empty.setHorizontalAlignment(Element.ALIGN_CENTER);
            table.addCell(empty);
            return table;
        }
        for (DepartmentReportVO d : departments) {
            addRow(table, cellFont,
                    safe(d.getDeptName()), str(d.getDeviceCount()), str(d.getPatrolCount()),
                    str(d.getTransportCount()), str(d.getPlanCount()), str(d.getEventCount()),
                    str(d.getDataCount()), str(d.getTotalBusinessCount()));
        }
        return table;
    }

    private static void addHeader(PdfPTable table, Font font, String... cells) {
        for (String text : cells) {
            PdfPCell cell = new PdfPCell(new Phrase(text, font));
            cell.setBackgroundColor(new Color(64, 158, 255));
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            cell.setPadding(5);
            table.addCell(cell);
        }
    }

    private static void addRow(PdfPTable table, Font font, String... cells) {
        for (String text : cells) {
            PdfPCell cell = new PdfPCell(new Phrase(text == null ? "" : text, font));
            cell.setPadding(4);
            table.addCell(cell);
        }
    }

    /** 打包字体优先（resources/fonts/*.ttf|.otf|.ttc），fallback Windows/常见 Linux 字体 */
    private static BaseFont resolveBaseFont() throws IOException {
        String[] candidates = {
                "C:/Windows/Fonts/msyh.ttc,0",
                "C:/Windows/Fonts/simsun.ttc,0",
                "C:/Windows/Fonts/simhei.ttf",
                "/usr/share/fonts/truetype/wqy/wqy-zenhei.ttc,0",
                "/usr/share/fonts/opentype/noto/NotoSansCJK-Regular.ttc,0",
        };
        for (String path : candidates) {
            try {
                String real = path;
                File f = new File(path.split(",")[0]);
                if (f.exists()) {
                    return BaseFont.createFont(real, BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
                }
            } catch (Exception ignored) {
                // try next
            }
        }
        // 最后兜底：内置 Asian 字体映射（部分 OpenPDF 发行版可用）
        return BaseFont.createFont("STSong-Light", "UniGB-UCS2-H", BaseFont.NOT_EMBEDDED);
    }

    private static String granularityName(String g) {
        if (g == null) return "按日";
        return switch (g) {
            case "month" -> "按月";
            case "week" -> "按周";
            default -> "按日";
        };
    }

    private static String rate(BigDecimal value) {
        return (value == null ? BigDecimal.ZERO : value).stripTrailingZeros().toPlainString() + "%";
    }

    private static String str(long value) {
        return String.valueOf(value);
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}

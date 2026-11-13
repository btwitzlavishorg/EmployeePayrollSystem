package com.payroll.util;

import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.payroll.model.Employee;
import com.payroll.model.Payroll;
import com.payroll.model.PayrollDetail;

import java.awt.Color;
import java.io.File;
import java.io.FileOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Enterprise PDF Generator utilizing OpenPDF.
 * Creates high-fidelity monthly payslips and analytical executive reports.
 */
public class PdfGenerator {

    private static final Color PRIMARY_COLOR = new Color(24, 43, 73);      // Navy Blue
    private static final Color ACCENT_COLOR = new Color(41, 128, 185);     // Steel Blue
    private static final Color HEADER_BG = new Color(240, 244, 248);       // Light Ice Blue
    private static final Color BORDER_COLOR = new Color(210, 220, 230);

    private static final Font FONT_TITLE = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, PRIMARY_COLOR);
    private static final Font FONT_SUBTITLE = FontFactory.getFont(FontFactory.HELVETICA, 10, Color.DARK_GRAY);
    private static final Font FONT_SECTION = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, PRIMARY_COLOR);
    private static final Font FONT_HEADER = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Color.WHITE);
    private static final Font FONT_LABEL = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Color.DARK_GRAY);
    private static final Font FONT_VALUE = FontFactory.getFont(FontFactory.HELVETICA, 9, Color.BLACK);
    private static final Font FONT_NET_TITLE = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 13, PRIMARY_COLOR);
    private static final Font FONT_NET_VALUE = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, new Color(16, 124, 65)); // Emerald Green
    private static final Font FONT_FOOTER = FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 8, Color.GRAY);

    /**
     * Generates a high-quality monthly payslip PDF.
     */
    public static void generatePayslipPdf(Payroll payroll, Employee employee, File targetFile) throws Exception {
        if (targetFile.getParentFile() != null) {
            targetFile.getParentFile().mkdirs();
        }

        Document document = new Document(PageSize.A4, 36, 36, 36, 36);
        PdfWriter.getInstance(document, new FileOutputStream(targetFile));
        document.open();

        // 1. Company Header
        Paragraph pCompany = new Paragraph("PAYROLLCORP ENTERPRISE SOLUTIONS", FONT_TITLE);
        pCompany.setAlignment(Element.ALIGN_CENTER);
        document.add(pCompany);

        Paragraph pAddress = new Paragraph("Tech Innovation Hub, Block 4, Silicon City, Bengaluru, Karnataka - 560100\nEmail: payroll@payrollcorp.com | Phone: +91 (080) 4567-8900", FONT_SUBTITLE);
        pAddress.setAlignment(Element.ALIGN_CENTER);
        pAddress.setSpacingAfter(10);
        document.add(pAddress);

        // Title Banner
        PdfPTable bannerTable = new PdfPTable(1);
        bannerTable.setWidthPercentage(100);
        PdfPCell bannerCell = new PdfPCell(new Phrase("SALARY PAYSLIP FOR " + 
                DateTimeUtil.getMonthName(payroll.getPayrollMonth()).toUpperCase() + " " + payroll.getPayrollYear(), FONT_HEADER));
        bannerCell.setBackgroundColor(PRIMARY_COLOR);
        bannerCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        bannerCell.setPadding(6);
        bannerCell.setBorder(0);
        bannerTable.addCell(bannerCell);
        bannerTable.setSpacingAfter(15);
        document.add(bannerTable);

        // 2. Employee Metadata & Attendance Info (2-column layout)
        PdfPTable infoTable = new PdfPTable(4);
        infoTable.setWidthPercentage(100);
        infoTable.setWidths(new float[]{22, 28, 22, 28});

        addInfoRow(infoTable, "Employee Code:", payroll.getEmployeeCode(), "Designation:", payroll.getDesignation());
        addInfoRow(infoTable, "Employee Name:", payroll.getEmployeeName(), "Department:", payroll.getDepartmentName());
        addInfoRow(infoTable, "Bank Name:", (payroll.getBankName() != null ? payroll.getBankName() : "HDFC Bank"), 
                              "Bank A/C No:", (payroll.getBankAccountNo() != null ? payroll.getBankAccountNo() : "N/A"));
        addInfoRow(infoTable, "PAN Number:", (payroll.getPanNo() != null ? payroll.getPanNo() : "N/A"),
                              "Payment Status:", payroll.getStatus().getDisplayName());
        addInfoRow(infoTable, "Working Days:", String.valueOf(payroll.getWorkingDays()),
                              "Present Days:", String.valueOf(payroll.getPresentDays()));
        addInfoRow(infoTable, "Unpaid Leaves:", String.valueOf(payroll.getUnpaidLeaveDays()) + " Days",
                              "Payment Date:", DateTimeUtil.formatDate(payroll.getPaymentDate()));

        infoTable.setSpacingAfter(15);
        document.add(infoTable);

        // 3. Earnings & Deductions Tables (Side by Side)
        PdfPTable splitTable = new PdfPTable(2);
        splitTable.setWidthPercentage(100);
        splitTable.setWidths(new float[]{50, 50});

        // Left: Earnings Table
        PdfPTable earningsTable = new PdfPTable(2);
        earningsTable.setWidthPercentage(100);
        earningsTable.setWidths(new float[]{65, 35});

        PdfPCell earnHead = new PdfPCell(new Phrase("EARNINGS / ALLOWANCES", FONT_HEADER));
        earnHead.setBackgroundColor(ACCENT_COLOR);
        earnHead.setColspan(2);
        earnHead.setPadding(5);
        earnHead.setHorizontalAlignment(Element.ALIGN_CENTER);
        earningsTable.addCell(earnHead);

        // Add Basic Salary
        addTableRow(earningsTable, "Basic Salary", CurrencyFormatter.format(payroll.getBasicSalary()), false);

        List<PayrollDetail> earnings = new ArrayList<>();
        List<PayrollDetail> deductions = new ArrayList<>();
        if (payroll.getDetails() != null) {
            for (PayrollDetail d : payroll.getDetails()) {
                if (d.isEarning()) earnings.add(d);
                else deductions.add(d);
            }
        }

        for (PayrollDetail d : earnings) {
            addTableRow(earningsTable, d.getComponentName(), CurrencyFormatter.format(d.getAmount()), false);
        }

        addTableRow(earningsTable, "TOTAL GROSS EARNINGS", CurrencyFormatter.format(payroll.getGrossSalary()), true);

        // Right: Deductions Table
        PdfPTable deductionsTable = new PdfPTable(2);
        deductionsTable.setWidthPercentage(100);
        deductionsTable.setWidths(new float[]{65, 35});

        PdfPCell dedHead = new PdfPCell(new Phrase("DEDUCTIONS", FONT_HEADER));
        dedHead.setBackgroundColor(new Color(192, 57, 43)); // Crimson Red
        dedHead.setColspan(2);
        dedHead.setPadding(5);
        dedHead.setHorizontalAlignment(Element.ALIGN_CENTER);
        deductionsTable.addCell(dedHead);

        for (PayrollDetail d : deductions) {
            addTableRow(deductionsTable, d.getComponentName(), CurrencyFormatter.format(d.getAmount()), false);
        }

        // Fill empty rows if needed so both tables look balanced
        int rowDiff = earnings.size() - deductions.size();
        for (int i = 0; i < Math.abs(rowDiff); i++) {
            if (rowDiff > 0) {
                addTableRow(deductionsTable, " ", " ", false);
            } else {
                addTableRow(earningsTable, " ", " ", false);
            }
        }

        addTableRow(deductionsTable, "TOTAL DEDUCTIONS", CurrencyFormatter.format(payroll.getTotalDeductions()), true);

        // Put earnings and deductions into container
        PdfPCell cellLeft = new PdfPCell(earningsTable);
        cellLeft.setBorder(0);
        cellLeft.setPaddingRight(5);

        PdfPCell cellRight = new PdfPCell(deductionsTable);
        cellRight.setBorder(0);
        cellRight.setPaddingLeft(5);

        splitTable.addCell(cellLeft);
        splitTable.addCell(cellRight);
        splitTable.setSpacingAfter(15);
        document.add(splitTable);

        // 4. Net Salary Highlight Box
        PdfPTable netTable = new PdfPTable(2);
        netTable.setWidthPercentage(100);
        netTable.setWidths(new float[]{60, 40});

        PdfPCell netLabelCell = new PdfPCell();
        netLabelCell.setBackgroundColor(HEADER_BG);
        netLabelCell.setBorderColor(BORDER_COLOR);
        netLabelCell.setPadding(8);
        netLabelCell.addElement(new Paragraph("NET SALARY PAYABLE:", FONT_NET_TITLE));
        netLabelCell.addElement(new Paragraph(CurrencyFormatter.convertToWords(payroll.getNetSalary()), FONT_SUBTITLE));
        netTable.addCell(netLabelCell);

        PdfPCell netValCell = new PdfPCell(new Phrase(CurrencyFormatter.format(payroll.getNetSalary()), FONT_NET_VALUE));
        netValCell.setBackgroundColor(HEADER_BG);
        netValCell.setBorderColor(BORDER_COLOR);
        netValCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        netValCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        netValCell.setPadding(8);
        netTable.addCell(netValCell);

        netTable.setSpacingAfter(25);
        document.add(netTable);

        // 5. Sign-off & Compliance Signatures
        PdfPTable signTable = new PdfPTable(2);
        signTable.setWidthPercentage(100);
        signTable.setWidths(new float[]{50, 50});

        PdfPCell signEmp = new PdfPCell(new Paragraph("___________________________\nEmployee Signature\nDate: ____________", FONT_VALUE));
        signEmp.setBorder(0);
        signEmp.setHorizontalAlignment(Element.ALIGN_LEFT);

        PdfPCell signAuth = new PdfPCell(new Paragraph("___________________________\nAuthorized Signatory (HR / Finance)\nPayrollCorp Technologies", FONT_VALUE));
        signAuth.setBorder(0);
        signAuth.setHorizontalAlignment(Element.ALIGN_RIGHT);

        signTable.addCell(signEmp);
        signTable.addCell(signAuth);
        signTable.setSpacingAfter(20);
        document.add(signTable);

        // 6. Footer Disclaimer
        Paragraph pFooter = new Paragraph(
                "Note: This is an official computer-generated payslip generated on " +
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd-MMM-yyyy hh:mm a")) +
                ". All tax deductions are subject to year-end statutory audit.", FONT_FOOTER);
        pFooter.setAlignment(Element.ALIGN_CENTER);
        document.add(pFooter);

        document.close();
    }

    /**
     * Generates a generic tabular report in PDF format.
     */
    public static void generateReportPdf(String title, String subtitle, String[] headers, List<String[]> dataRows, File targetFile) throws Exception {
        if (targetFile.getParentFile() != null) {
            targetFile.getParentFile().mkdirs();
        }

        Document document = new Document(PageSize.A4.rotate(), 30, 30, 30, 30);
        PdfWriter.getInstance(document, new FileOutputStream(targetFile));
        document.open();

        Paragraph pTitle = new Paragraph(title, FONT_TITLE);
        pTitle.setAlignment(Element.ALIGN_CENTER);
        document.add(pTitle);

        if (subtitle != null && !subtitle.isEmpty()) {
            Paragraph pSub = new Paragraph(subtitle + " | Generated: " + DateTimeUtil.formatDate(java.time.LocalDate.now()), FONT_SUBTITLE);
            pSub.setAlignment(Element.ALIGN_CENTER);
            pSub.setSpacingAfter(12);
            document.add(pSub);
        }

        PdfPTable table = new PdfPTable(headers.length);
        table.setWidthPercentage(100);

        // Headers
        for (String h : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(h, FONT_HEADER));
            cell.setBackgroundColor(PRIMARY_COLOR);
            cell.setPadding(5);
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            table.addCell(cell);
        }

        // Rows
        boolean alternate = false;
        for (String[] row : dataRows) {
            Color rowBg = alternate ? HEADER_BG : Color.WHITE;
            for (String val : row) {
                PdfPCell cell = new PdfPCell(new Phrase(val != null ? val : "", FONT_VALUE));
                cell.setBackgroundColor(rowBg);
                cell.setPadding(4);
                cell.setBorderColor(BORDER_COLOR);
                table.addCell(cell);
            }
            alternate = !alternate;
        }

        document.add(table);
        document.close();
    }

    private static void addInfoRow(PdfPTable table, String label1, String val1, String label2, String val2) {
        PdfPCell c1 = new PdfPCell(new Phrase(label1, FONT_LABEL));
        c1.setBackgroundColor(HEADER_BG);
        c1.setBorderColor(BORDER_COLOR);
        c1.setPadding(4);

        PdfPCell c2 = new PdfPCell(new Phrase(val1 != null ? val1 : "N/A", FONT_VALUE));
        c2.setBorderColor(BORDER_COLOR);
        c2.setPadding(4);

        PdfPCell c3 = new PdfPCell(new Phrase(label2, FONT_LABEL));
        c3.setBackgroundColor(HEADER_BG);
        c3.setBorderColor(BORDER_COLOR);
        c3.setPadding(4);

        PdfPCell c4 = new PdfPCell(new Phrase(val2 != null ? val2 : "N/A", FONT_VALUE));
        c4.setBorderColor(BORDER_COLOR);
        c4.setPadding(4);

        table.addCell(c1);
        table.addCell(c2);
        table.addCell(c3);
        table.addCell(c4);
    }

    private static void addTableRow(PdfPTable table, String name, String amount, boolean isTotal) {
        Font fontName = isTotal ? FONT_SECTION : FONT_VALUE;
        Font fontAmt = isTotal ? FONT_SECTION : FONT_VALUE;
        Color bg = isTotal ? HEADER_BG : Color.WHITE;

        PdfPCell cName = new PdfPCell(new Phrase(name, fontName));
        cName.setBackgroundColor(bg);
        cName.setPadding(4);
        cName.setBorderColor(BORDER_COLOR);

        PdfPCell cAmt = new PdfPCell(new Phrase(amount, fontAmt));
        cAmt.setBackgroundColor(bg);
        cAmt.setHorizontalAlignment(Element.ALIGN_RIGHT);
        cAmt.setPadding(4);
        cAmt.setBorderColor(BORDER_COLOR);

        table.addCell(cName);
        table.addCell(cAmt);
    }
}

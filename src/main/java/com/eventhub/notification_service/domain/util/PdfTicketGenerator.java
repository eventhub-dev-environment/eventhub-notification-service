package com.eventhub.notification_service.domain.util;

import com.eventhub.notification_service.infrastructure.adapter.out.feign.dto.EventResponseDto;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Image;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Map;

public class PdfTicketGenerator {

    public static byte[] generateTicketPdf(
            String userName,
            String bookingId,
            EventResponseDto eventData,
            List<Map<String, Object>> tickets
    ) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4, 36, 36, 36, 36);
            PdfWriter.getInstance(document, out);
            document.open();

            // Estilos de Fuentes
            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 20, Color.WHITE);
            Font subTitleFont = FontFactory.getFont(FontFactory.HELVETICA, 12, Color.WHITE);
            Font bodyBold = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, new Color(15, 23, 42));
            Font bodyNormal = FontFactory.getFont(FontFactory.HELVETICA, 11, new Color(51, 65, 85));

            // Banner Superior con el color del evento
            PdfPTable headerTable = new PdfPTable(1);
            headerTable.setWidthPercentage(100);

            String primaryColorHex = "#0f172a";
            if (eventData != null && eventData.getBranding() != null && eventData.getBranding().getPrimaryColor() != null) {
                primaryColorHex = eventData.getBranding().getPrimaryColor();
            }
            Color primaryColor = Color.decode(primaryColorHex);

            PdfPCell headerCell = new PdfPCell();
            headerCell.setBackgroundColor(primaryColor);
            headerCell.setPadding(20);
            headerCell.setHorizontalAlignment(Element.ALIGN_CENTER);

            String eventTitle = eventData != null ? eventData.getTitle() : "EventHub Event";
            String eventLocation = eventData != null ? eventData.getLocation() : "Ubicación";

            Paragraph pTitle = new Paragraph(eventTitle, titleFont);
            pTitle.setAlignment(Element.ALIGN_CENTER);
            Paragraph pLoc = new Paragraph(eventLocation, subTitleFont);
            pLoc.setAlignment(Element.ALIGN_CENTER);

            headerCell.addElement(pTitle);
            headerCell.addElement(pLoc);
            headerTable.addCell(headerCell);
            document.add(headerTable);

            // Información General
            document.add(new Paragraph(" ", bodyNormal));
            document.add(new Paragraph("Titular de la Entrada: " + userName, bodyBold));
            document.add(new Paragraph("Reserva ID: " + bookingId, bodyNormal));
            document.add(new Paragraph(" ", bodyNormal));

            // Renderizado de cada Ticket y su QR
            for (Map<String, Object> t : tickets) {
                String ticketId = (String) t.get("ticketId");
                String tierName = (String) t.get("tierName");
                byte[] qrBytes = (byte[]) t.get("qrBytes");

                PdfPTable ticketTable = new PdfPTable(1);
                ticketTable.setWidthPercentage(100);

                PdfPCell cell = new PdfPCell();
                cell.setPadding(15);
                cell.setBorderColor(new Color(203, 213, 225));

                cell.addElement(new Paragraph("Categoría: " + tierName, bodyBold));
                cell.addElement(new Paragraph("ID Ticket: " + ticketId, bodyNormal));

                if (qrBytes != null) {
                    Image qrImg = Image.getInstance(qrBytes);
                    qrImg.setAlignment(Element.ALIGN_CENTER);
                    qrImg.scaleToFit(150, 150);
                    cell.addElement(qrImg);
                }

                ticketTable.addCell(cell);
                document.add(ticketTable);
                document.add(new Paragraph(" ", bodyNormal));
            }

            document.close();
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Error al generar el PDF del ticket", e);
        }
    }
}
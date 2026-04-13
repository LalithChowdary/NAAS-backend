package com.naas.backend.report.dto;

import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WhoReceivedWhatResponse {
    private LocalDate deliveryDate;
    private String customerName;
    private String customerAddress;
    private String publicationName;
    private String status;
}

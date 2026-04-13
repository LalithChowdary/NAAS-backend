package com.naas.backend;

import com.naas.backend.delivery.repository.DeliveryRecordRepository;
import com.naas.backend.delivery.entity.DeliveryRecord;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

import java.util.List;

public class MarkDeliveriesDeliveredCli {

    public static void main(String[] args) {
        System.out.println("Starting CLI to mark all delivery records as DELIVERED...");

        ConfigurableApplicationContext context = new SpringApplicationBuilder(BackendApplication.class)
                .web(WebApplicationType.NONE)
                .run(args);

        DeliveryRecordRepository repo = context.getBean(DeliveryRecordRepository.class);
        
        List<DeliveryRecord> records = repo.findAll();
        int count = 0;
        for (DeliveryRecord record : records) {
            if (record.getStatus() != DeliveryRecord.DeliveryStatus.DELIVERED) {
                record.setStatus(DeliveryRecord.DeliveryStatus.DELIVERED);
                repo.save(record);
                count++;
            }
        }

        System.out.println("\n✅ " + count + " delivery records marked as DELIVERED.");
        
        context.close();
        System.exit(0);
    }
}

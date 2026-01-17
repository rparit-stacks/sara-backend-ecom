package com.sara.ecom.dto;

import com.sara.ecom.entity.SentMessage;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SentMessageDto {
    private Long id;
    private String recipientNumber;
    private String messageContent;
    private SentMessage.MessageType messageType;
    private SentMessage.Status status;
    private String wasenderResponse;
    private Long orderId;
    private LocalDateTime sentAt;
    private LocalDateTime createdAt;
}

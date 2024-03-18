package dev.handsup.notification.service;

import org.springframework.stereotype.Service;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;

import dev.handsup.auction.domain.Auction;
import dev.handsup.common.exception.NotFoundException;
import dev.handsup.common.exception.ValidationException;
import dev.handsup.notification.domain.NotificationType;
import dev.handsup.notification.dto.request.SaveFCMTokenRequest;
import dev.handsup.notification.exception.NotificationErrorCode;
import dev.handsup.notification.repository.FCMTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@Service
public class FCMService {

	private final FCMTokenRepository fcmTokenRepository;
	private final FirebaseMessaging firebaseMessaging;
	private final NotificationService notificationService;

	public void sendMessage(
		String senderEmail,
		String senderNickname,
		String receiverEmail,
		NotificationType notificationType,
		Auction auction
	) {
		if (!fcmTokenRepository.hasKey(receiverEmail)) {
			throw new NotFoundException(NotificationErrorCode.NOT_FOUND_FCM_TOKEN);
		}

		if (notificationType.equals(NotificationType.CANCELED_PURCHASE_TRADING) ||
			notificationType.equals(NotificationType.PURCHASE_WINNING)) {
			senderNickname = "";
		}

		String fcmToken = getFcmToken(receiverEmail);
		Message message = Message.builder()
			.putData("title", notificationType.getTitle())
			.putData("content", senderNickname + notificationType.getContent())
			.setToken(fcmToken)
			.build();

		send(message, receiverEmail);

		notificationService.saveNotification(
			senderEmail, receiverEmail, notificationType.getContent(), notificationType, auction
		);
	}

	private void send(Message message, String receiverEmail) {
		try {
			firebaseMessaging.send(message);
			log.info("Sent message: {}, to: {}", message, receiverEmail);
		} catch (FirebaseMessagingException e) {
			throw new ValidationException(e.getMessage());
		}
	}

	private String getFcmToken(String receiverEmail) {
		String fcmToken = fcmTokenRepository.getFcmToken(receiverEmail);
		if (fcmToken == null) {
			throw new NotFoundException(NotificationErrorCode.NOT_FOUND_FCM_TOKEN);
		}
		return fcmToken;
	}

	public void saveFcmToken(String userEmail, SaveFCMTokenRequest request) {
		fcmTokenRepository.saveFcmToken(userEmail, request.fcmToken());
	}

	public void deleteFcmToken(String email) {
		fcmTokenRepository.deleteFcmToken(email);
	}
}

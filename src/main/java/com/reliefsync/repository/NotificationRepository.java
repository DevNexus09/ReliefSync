package com.reliefsync.repository;

import com.reliefsync.model.Notification;
import java.util.List;

public interface NotificationRepository {
  List<Notification> findByUserId(long userId);

  List<Notification> findUnreadByUserId(long userId);

  long save(Notification notification);

  void markRead(long notificationId);
}

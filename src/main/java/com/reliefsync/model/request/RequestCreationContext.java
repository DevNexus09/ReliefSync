package com.reliefsync.model.request;

import com.reliefsync.security.UserSession;
import java.time.LocalDateTime;

public record RequestCreationContext(UserSession session, LocalDateTime timestamp) {}

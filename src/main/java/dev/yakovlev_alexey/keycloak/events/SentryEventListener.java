package dev.yakovlev_alexey.keycloak.events;

import java.util.HashMap;
import java.util.Map;

import org.keycloak.events.Event;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.admin.AdminEvent;

import dev.yakovlev_alexey.keycloak.sentry.EventSource;
import io.sentry.Hub;
import io.sentry.SentryEvent;
import io.sentry.SentryLevel;
import io.sentry.protocol.Message;
import io.sentry.protocol.User;

public class SentryEventListener implements EventListenerProvider {
	private final Hub hub;
	private final boolean errorsOnly;

	SentryEventListener(Hub hub, boolean errorsOnly) {
		this.hub = hub;
		this.errorsOnly = errorsOnly;
	}

	@Override
	public void close() {
	}

	@Override
	public void onEvent(Event event) {
		if (errorsOnly && event.getError() == null) {
			return;
		}

		SentryEvent sentryEvent = new SentryEvent();

		sentryEvent.setMessage(getMessage(event));
		sentryEvent.setLevel(getLevel(event));
		sentryEvent.setUser(getUser(event));
		sentryEvent.setExtras(getExtras(event));

		sentryEvent.setTag("type", event.getType().toString());
		sentryEvent.setTag("source", EventSource.COMMON.toString());

		hub.captureEvent(sentryEvent);
	}

	@Override
	public void onEvent(AdminEvent event, boolean includeRepresentation) {
		SentryEvent sentryEvent = new SentryEvent();

		sentryEvent.setMessage(getMessage(event));
		sentryEvent.setLevel(getLevel(event));
		sentryEvent.setUser(getUser(event));
		sentryEvent.setExtras(getExtras(event, includeRepresentation));

		sentryEvent.setTag("type", event.getOperationType().toString());
		sentryEvent.setTag("source", EventSource.ADMIN.toString());

		hub.captureEvent(sentryEvent);

	}

	private Message getMessage(Event event) {
		Message message = new Message();

		if (errorsOnly) {
			message.setMessage(event.getError());
		} else {
			message.setMessage(event.getType().toString());
		}

		return message;
	}

	private Message getMessage(AdminEvent event) {
		Message message = new Message();

		if (errorsOnly) {
			message.setMessage(event.getError());
		} else {
			message.setMessage(event.getOperationType().toString());
		}

		return message;
	}

	private SentryLevel getLevel(String error) {
		return error == null ? SentryLevel.INFO : SentryLevel.ERROR;
	}

	private SentryLevel getLevel(Event event) {
		return getLevel(event.getError());
	}

	private SentryLevel getLevel(AdminEvent event) {
		return getLevel(event.getError());
	}

	private User getUser(String userId) {
		if (userId == null) {
			return null;
		}

		User user = new User();
		user.setId(userId);

		return user;
	}

	private User getUser(Event event) {
		return getUser(event.getUserId());
	}

	private User getUser(AdminEvent event) {
		return getUser(event.getAuthDetails().getUserId());
	}

	private Map<String, Object> getExtras(Event event) {
		HashMap<String, Object> extras = new HashMap<>();
		extras.putAll(event.getDetails());

		extras.put("realmId", event.getRealmId());
		extras.put("clientId", event.getClientId());
		extras.put("sessionId", event.getSessionId());
		extras.put("ipAddress", event.getIpAddress());

		return extras;
	}

	private Map<String, Object> getExtras(AdminEvent event, boolean includeRepresentation) {
		HashMap<String, Object> extras = new HashMap<>();

		extras.put("realmId", event.getRealmId());
		extras.put("clientId", event.getAuthDetails().getClientId());
		extras.put("ipAddress", event.getAuthDetails().getIpAddress());
		extras.put("resourcePath", event.getResourcePath());
		extras.put("resourceType", event.getResourceTypeAsString());

		if (includeRepresentation) {
			extras.put("representation", event.getRepresentation());
		}

		return extras;
	}
}
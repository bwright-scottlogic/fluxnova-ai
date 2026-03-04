package com.fluxnova.scottlogic.mcp.tools;

import org.finos.fluxnova.bpm.engine.AuthorizationService;
import org.finos.fluxnova.bpm.engine.authorization.Authorization;
import org.finos.fluxnova.bpm.engine.authorization.Permissions;
import org.finos.fluxnova.bpm.engine.authorization.Resources;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class AuthorizationTools {

    private final AuthorizationService authorizationService;

    public AuthorizationTools(AuthorizationService authorizationService) {
        this.authorizationService = authorizationService;
    }

    @Tool(description = "Check whether a user (optionally in given groups) has a specific permission on a resource type. " +
            "Supported permissions: READ, UPDATE, CREATE, DELETE, ALL. " +
            "Supported resources: PROCESS_DEFINITION, PROCESS_INSTANCE, TASK, DEPLOYMENT, USER, GROUP, TENANT, AUTHORIZATION, BATCH.")
    public boolean isUserAuthorized(
            @ToolParam(description = "The user ID to check authorization for") String userId,
            @ToolParam(description = "Comma-separated list of group IDs the user belongs to (may be empty)", required = false) String groupIds,
            @ToolParam(description = "The permission to check, e.g. READ, UPDATE, CREATE, DELETE, ALL") String permission,
            @ToolParam(description = "The resource type to check against, e.g. PROCESS_DEFINITION, TASK") String resourceType,
            @ToolParam(description = "Optional specific resource ID to scope the check to", required = false) String resourceId) {

        List<String> groups = (groupIds != null && !groupIds.isBlank())
                ? List.of(groupIds.split(","))
                : List.of();

        Permissions perm = resolvePermission(permission);
        Resources resource = resolveResource(resourceType);

        if (resourceId != null && !resourceId.isBlank()) {
            return authorizationService.isUserAuthorized(userId, groups, perm, resource, resourceId);
        } else {
            return authorizationService.isUserAuthorized(userId, groups, perm, resource);
        }
    }

    @Tool(description = "List all authorization entries, optionally filtered by user ID or group ID.")
    public List<Map<String, String>> listAuthorizations(
            @ToolParam(description = "Optional user ID to filter authorizations by", required = false) String userId,
            @ToolParam(description = "Optional group ID to filter authorizations by", required = false) String groupId) {

        var query = authorizationService.createAuthorizationQuery();
        if (userId != null && !userId.isBlank()) {
            query.userIdIn(userId);
        }
        if (groupId != null && !groupId.isBlank()) {
            query.groupIdIn(groupId);
        }

        return query.list().stream()
                .map(a -> Map.of(
                        "id", a.getId() != null ? a.getId() : "",
                        "userId", a.getUserId() != null ? a.getUserId() : "",
                        "groupId", a.getGroupId() != null ? a.getGroupId() : "",
                        "resourceType", String.valueOf(a.getResourceType()),
                        "resourceId", a.getResourceId() != null ? a.getResourceId() : "",
                        "authorizationType", String.valueOf(a.getAuthorizationType())))
                .collect(Collectors.toList());
    }

    @Tool(description = "Delete an authorization entry by its ID.")
    public void deleteAuthorization(
            @ToolParam(description = "The authorization ID to delete") String authorizationId) {

        authorizationService.deleteAuthorization(authorizationId);
    }

    // ---- helpers ----

    private Permissions resolvePermission(String name) {
        return switch (name.toUpperCase()) {
            case "READ" -> Permissions.READ;
            case "UPDATE" -> Permissions.UPDATE;
            case "CREATE" -> Permissions.CREATE;
            case "DELETE" -> Permissions.DELETE;
            default -> Permissions.ALL;
        };
    }

    private Resources resolveResource(String name) {
        return switch (name.toUpperCase()) {
            case "PROCESS_DEFINITION" -> Resources.PROCESS_DEFINITION;
            case "PROCESS_INSTANCE" -> Resources.PROCESS_INSTANCE;
            case "TASK" -> Resources.TASK;
            case "DEPLOYMENT" -> Resources.DEPLOYMENT;
            case "USER" -> Resources.USER;
            case "GROUP" -> Resources.GROUP;
            case "TENANT" -> Resources.TENANT;
            case "AUTHORIZATION" -> Resources.AUTHORIZATION;
            case "BATCH" -> Resources.BATCH;
            default -> Resources.PROCESS_DEFINITION;
        };
    }
}

package com.example.student.security;

/** Keeps token validation backward compatible while supporting server-side invalidation. */
public class PermissionVersionPolicy {

    public boolean isCurrent(Integer tokenVersion, Integer serverVersion) {
        return serverVersion == null || tokenVersion == null || tokenVersion.equals(serverVersion);
    }
}

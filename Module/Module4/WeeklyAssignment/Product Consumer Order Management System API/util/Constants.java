package org.nbfc.productwa.util;

/**
 * Application-wide constants.
 */
public final class Constants {

    private Constants() {
    }

    public static final String BEARER_PREFIX = "Bearer ";
    public static final String AUTHORIZATION_HEADER = "Authorization";

    public static final String ROLE_USER = "ROLE_USER";
    public static final String ROLE_ADMIN = "ROLE_ADMIN";

    public static final int DEFAULT_PAGE_SIZE = 10;
    public static final String DEFAULT_PAGE_NUMBER = "0";
    public static final String DEFAULT_SORT_BY = "id";
}

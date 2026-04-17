package com.metumxs.filmlibraryapi.validation;

public final class ValidationConstants
{
    private ValidationConstants()
    {
    }

    // --- MOVIES ---
    public static final int MOVIE_TITLE_MAX_LENGTH = 255;
    public static final int MOVIE_DESC_MAX_LENGTH = 2000;
    public static final int MOVIE_MIN_YEAR = 1888;
    public static final int MOVIE_COUNTRY_MAX_LENGTH = 100;

    // --- USERS ---
    public static final int USER_NAME_MAX_LENGTH = 100;
    public static final int USER_EMAIL_MAX_LENGTH = 255;
    public static final int PASSWORD_MIN_LENGTH = 8;
    public static final int PASSWORD_MAX_LENGTH = 255;

    // --- RATINGS ---
    public static final int RATING_MIN_VALUE = 1;
    public static final int RATING_MAX_VALUE = 10;
}
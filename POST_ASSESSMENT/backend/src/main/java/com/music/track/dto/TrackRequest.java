package com.music.track.dto;

import java.util.Date;

public record TrackRequest(String title,
                           String albumName,
                           Date releaseDate,
                           Integer playCount) {
}

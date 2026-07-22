package com.music.track.service;

import com.music.track.dto.TrackRequest;
import com.music.track.model.Track;

import java.text.ParseException;
import java.util.List;

public interface TrackService {
    Track createTrack(TrackRequest trackRequest);

    Track getTracksByTitle(String title);
    List<Track> getAllTracks();

    void deleteTrack(Long trackId);
}

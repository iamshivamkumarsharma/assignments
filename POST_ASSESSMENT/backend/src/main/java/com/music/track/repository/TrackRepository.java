package com.music.track.repository;

import com.music.track.model.Track;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Date;

@Repository
public interface TrackRepository extends JpaRepository<Track, Long> {

    Track findByTitle(String title);
}

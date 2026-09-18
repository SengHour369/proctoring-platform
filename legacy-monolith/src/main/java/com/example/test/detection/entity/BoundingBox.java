package com.example.test.detection.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.Setter;

/**
 * Region of interest inside an analysed frame, stored as normalised (0..1) coordinates so it
 * survives a change of capture resolution.
 */
@Embeddable
@Getter
@Setter
public class BoundingBox {

    @Column(name = "box_x")
    private Double x;

    @Column(name = "box_y")
    private Double y;

    @Column(name = "box_width")
    private Double width;

    @Column(name = "box_height")
    private Double height;
}

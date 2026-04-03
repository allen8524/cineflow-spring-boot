package com.cineflow.dto;

import com.cineflow.domain.ScheduleSeat;
import com.cineflow.domain.SeatType;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SeatViewDto {

    private Long scheduleSeatId;
    private String seatRow;
    private Integer seatNumber;
    private String seatCode;
    private SeatType seatType;
    private String seatTypeLabel;
    private boolean reserved;
    private boolean held;
    private boolean active;
    private Integer price;

    public static SeatViewDto from(ScheduleSeat scheduleSeat, int price) {
        return SeatViewDto.builder()
                .scheduleSeatId(scheduleSeat.getId())
                .seatRow(scheduleSeat.getSeatTemplate().getSeatRow())
                .seatNumber(scheduleSeat.getSeatTemplate().getSeatNumber())
                .seatCode(scheduleSeat.getSeatTemplate().getSeatCode())
                .seatType(scheduleSeat.getSeatTemplate().getSeatType())
                .seatTypeLabel(scheduleSeat.getSeatTemplate().getSeatType().getLabel())
                .reserved(scheduleSeat.isReserved())
                .held(scheduleSeat.isHeld())
                .active(scheduleSeat.getSeatTemplate().isActive())
                .price(price)
                .build();
    }

    public boolean isSelectable() {
        return active && !reserved && !held;
    }

    public String getSeatCssClass() {
        StringBuilder cssClass = new StringBuilder("seat");
        if (seatType == SeatType.PREMIUM) {
            cssClass.append(" premium");
        }
        if (seatType == SeatType.COUPLE) {
            cssClass.append(" couple");
        }
        if (!active) {
            cssClass.append(" disabled-seat");
        }
        if (reserved) {
            cssClass.append(" reserved");
        }
        if (held) {
            cssClass.append(" held");
        }
        return cssClass.toString();
    }
}

package com.ssafy.s14p11c204.server.domain.game.dto;

import javax.management.AttributeNotFoundException;
import java.util.Optional;

public class GyeongdoRecord {
    public final long id;
    public final int policeWin;
    public final int policeLose;
    public final int imprisoningCnt;
    public final int thiefWin;
    public final int thiefLose;
    public final int rescuingCnt;
    public final double policeWinRate;
    public final double thiefWinRate;
    public final double totalWinRate;
    public final double imprisoningRate;
    public final double rescuingRate;


    public GyeongdoRecord(Long id, Integer policeWin, Integer policeLose, Integer imprisoningCnt, Integer thiefWin, Integer thiefLose, Integer rescuingCnt) {
        this.id = id != null ? id : 0L;
        this.policeWin = Optional.ofNullable(policeWin).orElse(0);
        this.policeLose = Optional.ofNullable(policeLose).orElse(0);
        this.imprisoningCnt = Optional.ofNullable(imprisoningCnt).orElse(0);
        this.thiefWin = Optional.ofNullable(thiefWin).orElse(0);
        this.thiefLose = Optional.ofNullable(thiefLose).orElse(0);
        this.rescuingCnt = Optional.ofNullable(rescuingCnt).orElse(0);

        int policeTotal = this.policeWin + this.policeLose;
        int thiefTotal = this.thiefWin + this.thiefLose;
        this.policeWinRate = policeTotal != 0 ? (double) this.policeWin / policeTotal : 0.0;
        this.thiefWinRate = thiefTotal != 0 ? (double) this.thiefWin / thiefTotal : 0.0;
        this.totalWinRate = this.policeWin + this.thiefWin != 0 ? (double) (this.policeWin + this.thiefWin) / (policeTotal + thiefTotal) : 0.0;
        this.imprisoningRate = policeTotal != 0 ? (double) this.imprisoningCnt / policeTotal : 0.0;
        this.rescuingRate = thiefTotal != 0 ? (double) this.rescuingCnt / thiefTotal : 0.0;
    }

    private GyeongdoRecord(Builder builder) {
        this.id = builder.id;
        if (builder.policeWin == null || builder.policeLose == null || builder.imprisoningCnt == null || builder.thiefWin == null || builder.thiefLose == null || builder.rescuingCnt == null) {
            throw new IllegalStateException("특정 변수가 null이어서 경도 기록 생성에 실패했습니다."); // NPE 대신 404
        }
        this.policeWin = builder.policeWin;
        this.policeLose = builder.policeLose;
        this.imprisoningCnt = builder.imprisoningCnt;
        this.thiefWin = builder.thiefWin;
        this.thiefLose = builder.thiefLose;
        this.rescuingCnt = builder.rescuingCnt;

        int policeTotal = policeWin + policeLose;
        int thiefTotal = thiefWin + thiefLose;
        this.policeWinRate = policeTotal != 0 ? (double) policeWin / policeTotal : 0.0;
        this.thiefWinRate = thiefTotal != 0 ? (double) thiefWin / thiefTotal : 0.0;
        this.totalWinRate = policeWin + thiefWin != 0 ? (double) (policeWin + thiefWin) / (policeTotal + thiefTotal) : 0.0;
        this.imprisoningRate = policeTotal != 0 ? (double) imprisoningCnt / policeTotal : 0.0;
        this.rescuingRate = thiefTotal != 0 ? (double) rescuingCnt / thiefTotal : 0.0;
    }

    public static class Builder {
        private final long id;
        private Integer policeWin;
        private Integer policeLose;
        private Integer imprisoningCnt;
        private Integer thiefWin;
        private Integer thiefLose;
        private Integer rescuingCnt;

        public Builder(long id) {
            this.id = id;
        }

        public Builder policeWin(Integer policeWin) {
            this.policeWin = policeWin;
            return this;
        }

        public Builder policeTotal(Integer policeTotal) {
            this.policeLose = policeTotal;
            return this;
        }

        public Builder imprisoningCnt(Integer imprisoningCnt) {
            this.imprisoningCnt = imprisoningCnt;
            return this;
        }

        public Builder thiefWin(Integer thiefWin) {
            this.thiefWin = thiefWin;
            return this;
        }

        public Builder thiefTotal(Integer thiefTotal) {
            this.thiefLose = thiefTotal;
            return this;
        }

        public Builder rescuingCnt(Integer rescuingCnt) {
            this.rescuingCnt = rescuingCnt;
            return this;
        }

        public GyeongdoRecord build() {
            return new GyeongdoRecord(this);
        }
    }
}

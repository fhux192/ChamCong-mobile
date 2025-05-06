package com.example.timekeeping;

public class AttendanceRecord {

    // Existing fields
    private String userId;
    private String date;
    private long inTime;
    private long outTime;
    private long durationMillis;
    private String inLocation;
    private String outLocation;
    private String companyName;

    // New fields
    private boolean late;       // True if check-in is after workStartHour
    private boolean earlyLeave; // True if check-out is before workEndHour

    // Constructors
    public AttendanceRecord() { }

    public AttendanceRecord(String userId, String date) {
        this.userId = userId;
        this.date = date;
    }

    // Existing getters and setters
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }
    public long getInTime() { return inTime; }
    public void setInTime(long inTime) { this.inTime = inTime; }
    public long getOutTime() { return outTime; }
    public void setOutTime(long outTime) { this.outTime = outTime; }
    public long getDurationMillis() { return durationMillis; }
    public void setDurationMillis(long durationMillis) { this.durationMillis = durationMillis; }
    public String getInLocation() { return inLocation; }
    public void setInLocation(String inLocation) { this.inLocation = inLocation; }
    public String getOutLocation() { return outLocation; }
    public void setOutLocation(String outLocation) { this.outLocation = outLocation; }
    public String getCompanyName() { return companyName; }
    public void setCompanyName(String companyName) { this.companyName = companyName; }

    // New getters and setters
    public boolean isLate() { return late; }
    public void setLate(boolean late) { this.late = late; }
    public boolean isEarlyLeave() { return earlyLeave; }
    public void setEarlyLeave(boolean earlyLeave) { this.earlyLeave = earlyLeave; }

    // Existing equals, hashCode, and toString methods remain unchanged
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof AttendanceRecord)) return false;
        AttendanceRecord other = (AttendanceRecord) obj;
        return userId != null && userId.equals(other.userId)
                && date != null && date.equals(other.date);
    }

    @Override
    public int hashCode() {
        int result = (userId != null) ? userId.hashCode() : 0;
        result = 31 * result + ((date != null) ? date.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "AttendanceRecord{" +
                "userId='" + userId + '\'' +
                ", date='" + date + '\'' +
                ", inTime=" + inTime +
                ", outTime=" + outTime +
                ", durationMillis=" + durationMillis +
                ", inLocation='" + inLocation + '\'' +
                ", outLocation='" + outLocation + '\'' +
                ", companyName='" + companyName + '\'' +
                ", late=" + late +
                ", earlyLeave=" + earlyLeave +
                '}';
    }
}
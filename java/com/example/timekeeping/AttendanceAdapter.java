package com.example.timekeeping;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AttendanceAdapter extends ArrayAdapter<AttendanceRecord> {

    private final LayoutInflater inflater;
    private final SimpleDateFormat hhmmss =
            new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
    private final MainActivityCPP host;
    private final boolean isManager;

    public AttendanceAdapter(Context ctx, List<AttendanceRecord> data, MainActivityCPP host, boolean isManager) {
        super(ctx, 0, data);
        this.inflater = LayoutInflater.from(ctx);
        this.host = host;
        this.isManager = isManager;
    }

    @NonNull
    @Override
    public View getView(int pos, @Nullable View convertView, @NonNull ViewGroup parent) {
        View v = convertView;
        if (v == null)
            v = inflater.inflate(R.layout.item_attendance, parent, false);

        AttendanceRecord r = getItem(pos);
        if (r == null) return v;

        TextView tvName = v.findViewById(R.id.tvName);
        TextView tvInOut = v.findViewById(R.id.tvInOut);

        // Lấy tên – nếu null thì hiển thị UID
        String name = host.getNameByUid(r.getUserId());
        if (name == null) name = r.getUserId();

        if (isManager && name != null)
            name = name + "  (UID: " + r.getUserId() + ')';

        tvName.setText(name);

        // Hiển thị thời gian vào, ra và tổng giờ làm
        String in = r.getInTime() == 0 ? "--" : hhmmss.format(new Date(r.getInTime()));
        String out = r.getOutTime() == 0 ? "--" : hhmmss.format(new Date(r.getOutTime()));
        String dur;
        if (r.getDurationMillis() <= 0) dur = "--";
        else {
            long s = r.getDurationMillis() / 1000;
            dur = String.format(Locale.getDefault(), "%02d:%02d:%02d", s / 3600, (s % 3600) / 60, s % 60);
        }

        tvInOut.setText("Vào  : " + in +
                "\nRa   : " + out +
                "\nTổng : " + dur);

        return v;
    }

    /**
     * Tính tổng số giờ làm trong tháng và thêm vào adapter để hiển thị
     */
    public String getTotalWorkingHours(List<AttendanceRecord> records) {
        long totalMillis = 0;

        for (AttendanceRecord record : records) {
            if (record.getInTime() != 0 && record.getOutTime() != 0) {
                totalMillis += (record.getOutTime() - record.getInTime());
            }
        }

        long totalSeconds = totalMillis / 1000;
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;

        return String.format(Locale.getDefault(), "%02d:%02d", hours, minutes);
    }
}

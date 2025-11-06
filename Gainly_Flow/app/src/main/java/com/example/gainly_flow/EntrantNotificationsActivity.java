package com.example.gainly_flow;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.FirebaseApp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

/** Single-file Notifications screen: activity + model + adapter. */
public class EntrantNotificationsActivity extends AppCompatActivity {

    // ------- replace with your real auth uid -------
    private static final String CURRENT_USER_ID = "22ae419f5bed11cd";
    // ------------------------------------------------

    private MaterialToolbar toolbar;
    private ListView list;
    private NotifAdapter adapter;

    private FirebaseFirestore fs;
    private ListenerRegistration reg;

    @Override protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FirebaseApp.initializeApp(this);
        setContentView(R.layout.activity_entrant_notifications);

        toolbar = findViewById(R.id.toolbar);
        list    = findViewById(R.id.listNotifications);

        if (toolbar != null) toolbar.setNavigationOnClickListener(v -> finish());

        fs = FirebaseFirestore.getInstance();
        adapter = new NotifAdapter(this, new ArrayList<>());
        list.setAdapter(adapter);

        // Mark read on row tap
        list.setOnItemClickListener((parent, view, pos, id) -> {
            Notif n = adapter.getItem(pos);
            if (n == null || n.id == null) return;
            fs.collection("notifications").document(n.id).update("read", true);
            adapter.notifyDataSetChanged();
        });
    }

    @Override
    protected void onStart() {
        super.onStart();

        // Order all notifications by timestamp (descending)
        reg = fs.collection("notifications")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener((snap, err) -> {
                    if (err != null) {
                        android.util.Log.e("NOTIF_DEBUG", "Error ordering by timestamp: ", err);
                        Toast.makeText(this, "Firestore error: " + err.getMessage(), Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (snap == null) {
                        android.util.Log.w("NOTIF_DEBUG", "Snapshot is null!");
                        return;
                    }

                    List<Notif> data = new ArrayList<>();
                    int count = 0;
                    for (DocumentSnapshot d : snap.getDocuments()) {
                        count++;
                        android.util.Log.d("NOTIF_DEBUG", "Doc " + count + ": " + d.getData());
                        data.add(Notif.from(d));
                    }

                    android.util.Log.i("NOTIF_DEBUG", "Fetched " + count + " notifications (ordered by timestamp).");
                    adapter.replace(data);
                });
    }


    @Override protected void onStop() {
        super.onStop();
        if (reg != null) { reg.remove(); reg = null; }
    }

    /* ---------------- small row model ---------------- */

    private static class Notif {
        final String id, title, message, type, eventId;
        final long timestamp;
        final boolean read;

        Notif(String id, String title, String message, String type,
              String eventId, long timestamp, boolean read) {
            this.id = id;
            this.title = nz(title, "Notification");
            this.message = nz(message, "");
            this.type = nz(type, "CUSTOM");
            this.eventId = eventId;
            this.timestamp = timestamp;
            this.read = read;
        }

        static Notif from(DocumentSnapshot d) {
            String id   = getStr(d.get("notificationId")); if (id.isEmpty()) id = d.getId();
            String t    = getStr(d.get("title"));
            String msg  = getStr(d.get("message"));
            String type = getStr(d.get("type"));
            String eid  = getStr(d.get("eventId"));
            Long ts     = getLong(d.get("timestamp"));
            Boolean r   = asBool(d.get("read"));
            return new Notif(id, t, msg, type, eid, ts == null ? 0L : ts, r != null && r);
        }

        static String nz(String s, String def){ return s==null||s.trim().isEmpty()?def:s; }
        static String getStr(Object o){ return o==null? "": String.valueOf(o); }
        static Long getLong(Object o){
            if (o instanceof Number) return ((Number)o).longValue();
            if (o instanceof String) try { return Long.parseLong((String)o);} catch(Exception ignored){}
            return null;
        }
        static Boolean asBool(Object o){
            if (o instanceof Boolean) return (Boolean)o;
            if (o instanceof String)  return "true".equalsIgnoreCase((String)o) || "1".equals(o);
            return false;
        }
    }

    /* ---------------- simple ArrayAdapter ---------------- */

    private class NotifAdapter extends ArrayAdapter<Notif> {
        NotifAdapter(Context ctx, List<Notif> data){ super(ctx, 0, data); }
        void replace(List<Notif> data){ clear(); addAll(data); notifyDataSetChanged(); }

        @Override public View getView(int pos, View convertView, ViewGroup parent) {
            View v = (convertView != null) ? convertView :
                    LayoutInflater.from(getContext()).inflate(R.layout.item_notification, parent, false);

            View colorBar       = v.findViewById(R.id.colorBar);
            TextView tvTime     = v.findViewById(R.id.tvTime);
            TextView tvTitle    = v.findViewById(R.id.tvTitle);
            TextView tvMessage  = v.findViewById(R.id.tvMessage);
            Button btnDetails   = v.findViewById(R.id.btnDetails);

            Notif n = getItem(pos);
            if (n == null) return v;

            tvTitle.setText(n.title);
            tvMessage.setText(n.message);
            tvTime.setText(formatAgo(n.timestamp));
            colorBar.setBackgroundColor(colorForType(n.type));

            // Dim read items a bit
            v.setAlpha(n.read ? 0.75f : 1f);

            // Details: show only if eventId exists
            if (n.eventId != null && !n.eventId.trim().isEmpty()) {
                btnDetails.setVisibility(View.VISIBLE);
                btnDetails.setOnClickListener(view -> {
                    Intent i = new Intent(getContext(), EventDetailActivity.class);
                    i.putExtra(EventDetailActivity.EXTRA_EVENT_ID, n.eventId);
                    getContext().startActivity(i);
                });
            } else {
                btnDetails.setVisibility(View.GONE);
                btnDetails.setOnClickListener(null);
            }
            return v;
        }

        private String formatAgo(long ts){
            if (ts<=0) return "";
            return DateUtils.getRelativeTimeSpanString(
                    ts, System.currentTimeMillis(),
                    DateUtils.MINUTE_IN_MILLIS,
                    DateUtils.FORMAT_ABBREV_RELATIVE).toString();
        }

        private int colorForType(String type){
            if ("SELECTED".equalsIgnoreCase(type) || "INVITE".equalsIgnoreCase(type)) return 0xFF1FBF75; // green
            if ("NOT_SELECTED".equalsIgnoreCase(type) || "DECLINED".equalsIgnoreCase(type)) return 0xFFE34B4B; // red
            if ("CLOSING_SOON".equalsIgnoreCase(type)) return 0xFF2C6FFF; // blue
            return 0xFF2C6FFF; // default/custom/info
        }
    }
}
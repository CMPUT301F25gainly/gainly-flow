package com.example.gainly_flow;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;
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

    private static final int STATUS_PENDING  = 0;
    private static final int STATUS_ACCEPTED = 1;
    private static final int STATUS_DECLINED = 2;

    private MaterialToolbar toolbar;
    private ListView list;
    private NotifAdapter adapter;
    private BottomNavigationView bottomNav;

    private FirebaseFirestore fs;
    private ListenerRegistration reg;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FirebaseApp.initializeApp(this);
        setContentView(R.layout.activity_entrant_notifications);

        toolbar   = findViewById(R.id.toolbar);
        list      = findViewById(R.id.listNotifications);
        bottomNav = findViewById(R.id.bottomNav);

        if (toolbar != null) toolbar.setNavigationOnClickListener(v -> finish());

        fs = FirebaseFirestore.getInstance();
        adapter = new NotifAdapter(this, new ArrayList<>());
        list.setAdapter(adapter);

        // Mark read on row tap
        list.setOnItemClickListener((parent, view, pos, id) -> {
            Notif n = adapter.getItem(pos);
            if (n == null || n.id == null) return;
            fs.collection("notifications").document(n.id).update("read", true);
            n.read = true;
            adapter.notifyDataSetChanged();
        });

        // Bottom navigation
        if (bottomNav != null) {
            bottomNav.setSelectedItemId(R.id.menu_notifications); // highlight current tab
            bottomNav.setOnItemSelectedListener(this::onNavItemSelected);
        }
    }

    private boolean onNavItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.menu_events) {
            Intent intent = new Intent(this, EntrantViewMain.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
            return true;
        } else if (id == R.id.menu_notifications) {
            return true;
        } else if (id == R.id.menu_profile) {
            Toast.makeText(this, "Profile coming soon", Toast.LENGTH_SHORT).show();
            return true;
        }
        return false;
    }

    @Override
    protected void onStart() {
        super.onStart();

        // If you want to scope to the current user, uncomment the whereEqualTo line.
        reg = fs.collection("notifications")
                //.whereEqualTo("recipientDeviceId", CURRENT_USER_ID)
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

    @Override
    protected void onStop() {
        super.onStop();
        if (reg != null) { reg.remove(); reg = null; }
    }

    /* ---------------- small row model ---------------- */

    private static class Notif {
        final String id, title, message, type, eventId, recipientId;
        final long timestamp;
        boolean read;
        int acceptStatus; // 0: pending, 1: accepted, 2: declined

        Notif(String id, String title, String message, String type,
              String eventId, String recipientId, long timestamp, boolean read, int acceptStatus) {
            this.id = id;
            this.title = nz(title, "Notification");
            this.message = nz(message, "");
            this.type = nz(type, "CUSTOM");           // WIN | LOSE | CUSTOM
            this.eventId = eventId;
            this.recipientId = recipientId;
            this.timestamp = timestamp;
            this.read = read;
            this.acceptStatus = acceptStatus;
        }

        static Notif from(DocumentSnapshot d) {
            String id   = getStr(d.get("notificationId")); if (id.isEmpty()) id = d.getId();
            String t    = getStr(d.get("title"));
            String msg  = getStr(d.get("message"));
            String type = getStr(d.get("type")); // WIN, LOSE, CUSTOM
            String eid  = getStr(d.get("eventId"));
            String rid  = getStr(d.get("recipientDeviceId")); // or recipientId in your schema
            Long ts     = getLong(d.get("timestamp"));
            Boolean r   = asBool(d.get("read"));
            Integer acc = getInt(d.get("acceptStatus"));
            return new Notif(
                    id, t, msg, type, eid, rid,
                    ts == null ? 0L : ts,
                    r != null && r,
                    acc == null ? STATUS_PENDING : acc
            );
        }

        static String nz(String s, String def){ return s==null||s.trim().isEmpty()?def:s; }
        static String getStr(Object o){ return o==null? "": String.valueOf(o); }
        static Long getLong(Object o){
            if (o instanceof Number) return ((Number)o).longValue();
            if (o instanceof String) try { return Long.parseLong((String)o);} catch(Exception ignored){}
            return null;
        }
        static Integer getInt(Object o){
            if (o instanceof Number) return ((Number)o).intValue();
            if (o instanceof String) try { return Integer.parseInt((String)o);} catch(Exception ignored){}
            return null;
        }
        static Boolean asBool(Object o){
            if (o instanceof Boolean) return (Boolean)o;
            if (o instanceof String)  return "true".equalsIgnoreCase((String)o) || "1".equals(o);
            return false;
        }

        boolean isWin(){ return "WIN".equalsIgnoreCase(type); }
        boolean isLose(){ return "LOSE".equalsIgnoreCase(type); }
        boolean isCustom(){ return "CUSTOM".equalsIgnoreCase(type); }
        boolean isPendingDecision(){ return acceptStatus == STATUS_PENDING; }
    }

    /* ---------------- simple ArrayAdapter ---------------- */

    private class NotifAdapter extends ArrayAdapter<Notif> {
        NotifAdapter(Context ctx, List<Notif> data){ super(ctx, 0, data); }
        void replace(List<Notif> data){ clear(); addAll(data); notifyDataSetChanged(); }

        @Override
        public View getView(int pos, View convertView, ViewGroup parent) {
            View v = (convertView != null) ? convertView :
                    LayoutInflater.from(getContext()).inflate(R.layout.item_notification, parent, false);

            View colorBar      = v.findViewById(R.id.colorBar);
            TextView tvTime    = v.findViewById(R.id.tvTime);
            TextView tvTitle   = v.findViewById(R.id.tvTitle);
            TextView tvMsg     = v.findViewById(R.id.tvMessage);
            TextView tvDecision= v.findViewById(R.id.tvDecision);
            Button btnAccept   = v.findViewById(R.id.btnAccept);
            Button btnDecline  = v.findViewById(R.id.btnDecline);

            Notif n = getItem(pos);
            if (n == null) return v;

            tvTitle.setText(n.title);
            tvMsg.setText(n.message);
            tvTime.setText(formatAgo(n.timestamp));

            // Dim read items a bit
            v.setAlpha(n.read ? 0.75f : 1f);

            // --- Color bar by type ---
            if (n.isWin()) {
                colorBar.setBackgroundColor(0xFF1FBF75); // green
            } else if (n.isLose()) {
                colorBar.setBackgroundColor(0xFFE34B4B); // red
            } else {
                colorBar.setBackgroundColor(0xFF2C6FFF); // blue (CUSTOM)
            }

            // Reset visibility for reused rows
            tvDecision.setVisibility(View.GONE);
            btnAccept.setVisibility(View.GONE);
            btnDecline.setVisibility(View.GONE);
            btnAccept.setOnClickListener(null);
            btnDecline.setOnClickListener(null);

            // --- Decision UI only for WIN type ---
            if (n.isWin()) {
                if (n.isPendingDecision()) {
                    // Show buttons
                    btnAccept.setVisibility(View.VISIBLE);
                    btnDecline.setVisibility(View.VISIBLE);

                    btnAccept.setOnClickListener(view -> {
                        fs.collection("notifications")
                                .document(n.id)
                                .update("acceptStatus", STATUS_ACCEPTED, "read", true)
                                .addOnFailureListener(e ->
                                        Toast.makeText(getContext(), "Update failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                        n.acceptStatus = STATUS_ACCEPTED;
                        n.read = true;
                        notifyDataSetChanged();
                    });

                    btnDecline.setOnClickListener(view -> {
                        fs.collection("notifications")
                                .document(n.id)
                                .update("acceptStatus", STATUS_DECLINED, "read", true)
                                .addOnFailureListener(e ->
                                        Toast.makeText(getContext(), "Update failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                        n.acceptStatus = STATUS_DECLINED;
                        n.read = true;
                        notifyDataSetChanged();
                    });
                } else {
                    // Show final state label
                    tvDecision.setVisibility(View.VISIBLE);
                    if (n.acceptStatus == STATUS_ACCEPTED) {
                        tvDecision.setText("Accepted");
                        tvDecision.setTextColor(0xFF1FBF75); // green
                    } else { // DECLINED
                        tvDecision.setText("Declined");
                        tvDecision.setTextColor(0xFFE34B4B); // red
                    }
                }
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
    }
}

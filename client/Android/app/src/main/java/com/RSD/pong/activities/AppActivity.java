package com.RSD.pong.activities;

import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.RSD.pong.R;
import com.RSD.pong.models.ChatItem;
import com.RSD.pong.utils.ChatAdapter;
import com.google.android.material.navigation.NavigationView;

import java.util.ArrayList;
import java.util.List;

public class AppActivity extends AppCompatActivity {

    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private RecyclerView recyclerChats;
    private ImageButton btnMenu;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_app);

        drawerLayout = findViewById(R.id.drawerLayout);
        navigationView = findViewById(R.id.navView);
        recyclerChats = findViewById(R.id.recyclerChats);
        btnMenu = findViewById(R.id.btnMenu);

        // setting RecyclerView up
        recyclerChats.setLayoutManager(new LinearLayoutManager(this));
        List<ChatItem> chatList = generateDummyChats();
        ChatAdapter adapter = new ChatAdapter(chatList);
        recyclerChats.setAdapter(adapter);

        // setting side panel
        navigationView.setNavigationItemSelectedListener(item -> {
            handleMenuClick(item);
            return true;
        });

        btnMenu.setOnClickListener(v -> drawerLayout.openDrawer(navigationView));
    }

    private List<ChatItem> generateDummyChats() {
        List<ChatItem> list = new ArrayList<>();
        list.add(new ChatItem(R.drawable.ico_1, "Alice", "Hey! How are you?"));
        list.add(new ChatItem(R.drawable.ico_1, "Bob", "Let's meet tomorrow."));
        list.add(new ChatItem(R.drawable.ico_1, "Charlie", "Check this out!"));
        return list;
    }

    private void handleMenuClick(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.nav_profile) {
            Toast.makeText(this, "Profile clicked", Toast.LENGTH_SHORT).show();
        } else if (id == R.id.nav_new_user) {
            Toast.makeText(this, "New User clicked", Toast.LENGTH_SHORT).show();
        } else if (id == R.id.nav_settings) {
            Toast.makeText(this, "Settings clicked", Toast.LENGTH_SHORT).show();
        }
        drawerLayout.closeDrawers();
    }

}
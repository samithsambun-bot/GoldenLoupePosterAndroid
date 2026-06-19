package com.goldenloupe.poster;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import org.json.JSONObject;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends Activity {
    private static final int SYNC_PORT = 45454;
    private static final String PREFS = "gold_prices";
    private final Handler handler = new Handler(Looper.getMainLooper());

    private boolean displayMode = false;
    private volatile boolean listening = false;
    private SharedPreferences prefs;
    private WifiManager.MulticastLock multicastLock;

    private PriceData prices = new PriceData();
    private Runnable syncRunnable;

    private TextView dateText;
    private TextView timeText;
    private TextView goldBuy;
    private TextView goldSellGram;
    private TextView goldSellKg;
    private TextView platinumBuy;
    private TextView platinumSellGram;
    private TextView platinumSellKg;
    private TextView silverBuy;
    private TextView silverSellGram;
    private TextView silverSellKg;

    private EditText goldBuyInput;
    private EditText goldSellGramInput;
    private EditText goldSellKgInput;
    private EditText platinumBuyInput;
    private EditText platinumSellGramInput;
    private EditText platinumSellKgInput;
    private EditText silverBuyInput;
    private EditText silverSellGramInput;
    private EditText silverSellKgInput;
    private CheckBox goldAvailable;
    private CheckBox platinumAvailable;
    private CheckBox silverAvailable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        prices = PriceData.fromPrefs(prefs);
        syncRunnable = () -> {
            if (!displayMode && goldBuyInput != null) {
                readInputs();
                prices.save(prefs);
                broadcastPrices();
            }
        };
        showModePicker();
    }

    @Override
    protected void onDestroy() {
        listening = false;
        if (multicastLock != null && multicastLock.isHeld()) {
            multicastLock.release();
        }
        super.onDestroy();
    }

    private void showModePicker() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        root.setPadding(dp(40), dp(40), dp(40), dp(40));
        root.setBackgroundColor(Color.rgb(245, 241, 233));

        ImageView logo = new ImageView(this);
        logo.setImageResource(getResources().getIdentifier("logo", "drawable", getPackageName()));
        logo.setAdjustViewBounds(true);
        root.addView(logo, new LinearLayout.LayoutParams(dp(220), dp(140)));

        TextView title = new TextView(this);
        title.setText("Golden Loupe Poster");
        title.setTextSize(34);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setTextColor(Color.rgb(92, 64, 51));
        title.setGravity(Gravity.CENTER);
        root.addView(title);

        Button control = modeButton("Tablet Control Mode");
        Button display = modeButton("TV Display Mode");
        root.addView(control);
        root.addView(display);

        control.setOnClickListener(v -> showControlMode());
        display.setOnClickListener(v -> showDisplayMode());
        setContentView(root);
    }

    private Button modeButton(String text) {
        Button button = new Button(this);
        button.setText(text);
        button.setTextSize(24);
        button.setTextColor(Color.WHITE);
        button.setBackgroundColor(Color.rgb(184, 150, 90));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dp(420), dp(78));
        params.setMargins(0, dp(24), 0, 0);
        button.setLayoutParams(params);
        return button;
    }

    private void showControlMode() {
        displayMode = false;

        ScrollView scroll = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(28), dp(22), dp(28), dp(22));
        root.setBackgroundColor(Color.rgb(238, 238, 238));
        scroll.addView(root);

        TextView title = heading("Tablet Control", 30);
        title.setGravity(Gravity.CENTER);
        root.addView(title);

        GridLayout grid = new GridLayout(this);
        grid.setColumnCount(3);
        root.addView(grid, new LinearLayout.LayoutParams(-1, -2));

        goldBuyInput = priceInput(prices.goldBuy);
        goldSellGramInput = priceInput(prices.goldSellGram);
        goldSellKgInput = priceInput(prices.goldSellKg);
        goldAvailable = productCard(grid, "Au99.99 Gold", goldBuyInput, goldSellGramInput, goldSellKgInput, prices.goldSellAvailable);

        platinumBuyInput = priceInput(prices.platinumBuy);
        platinumSellGramInput = priceInput(prices.platinumSellGram);
        platinumSellKgInput = priceInput(prices.platinumSellKg);
        platinumAvailable = productCard(grid, "PT999", platinumBuyInput, platinumSellGramInput, platinumSellKgInput, prices.platinumSellAvailable);

        silverBuyInput = priceInput(prices.silverBuy);
        silverSellGramInput = priceInput(prices.silverSellGram);
        silverSellKgInput = priceInput(prices.silverSellKg);
        silverAvailable = productCard(grid, "Silver", silverBuyInput, silverSellGramInput, silverSellKgInput, prices.silverSellAvailable);

        Button send = modeButton("Send Prices to TV");
        root.addView(send);
        send.setOnClickListener(v -> {
            readInputs();
            prices.save(prefs);
            broadcastPrices();
        });

        Button preview = modeButton("Open Display Preview");
        root.addView(preview);
        preview.setOnClickListener(v -> {
            readInputs();
            prices.save(prefs);
            showDisplayMode();
        });

        syncInputState();
        setupAutoSync();
        setContentView(scroll);
    }

    private CheckBox productCard(GridLayout grid, String name, EditText buy, EditText sellGram, EditText sellKg, boolean available) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(18), dp(16), dp(18), dp(16));
        card.setBackgroundColor(Color.rgb(255, 250, 241));

        TextView title = heading(name, 24);
        card.addView(title);
        card.addView(label("Buy-in price"));
        card.addView(buy);
        card.addView(label("Sell /Gram -- /克"));
        card.addView(sellGram);
        card.addView(label("Sell /KG -- /公斤"));
        card.addView(sellKg);

        CheckBox checkBox = new CheckBox(this);
        checkBox.setText("Sell available");
        checkBox.setTextSize(17);
        checkBox.setTextColor(Color.rgb(92, 64, 51));
        checkBox.setChecked(available);
        checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            syncInputState();
            scheduleSync();
        });
        card.addView(checkBox);

        GridLayout.LayoutParams params = new GridLayout.LayoutParams();
        params.width = 0;
        params.height = GridLayout.LayoutParams.WRAP_CONTENT;
        params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
        params.setMargins(dp(8), dp(8), dp(8), dp(8));
        grid.addView(card, params);
        return checkBox;
    }

    private void syncInputState() {
        if (goldSellGramInput == null) return;
        goldSellGramInput.setEnabled(goldAvailable.isChecked());
        goldSellKgInput.setEnabled(goldAvailable.isChecked());
        platinumSellGramInput.setEnabled(platinumAvailable.isChecked());
        platinumSellKgInput.setEnabled(platinumAvailable.isChecked());
        silverSellGramInput.setEnabled(silverAvailable.isChecked());
        silverSellKgInput.setEnabled(silverAvailable.isChecked());
    }

    private void setupAutoSync() {
        TextWatcher watcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                scheduleSync();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        };

        goldBuyInput.addTextChangedListener(watcher);
        goldSellGramInput.addTextChangedListener(watcher);
        goldSellKgInput.addTextChangedListener(watcher);
        platinumBuyInput.addTextChangedListener(watcher);
        platinumSellGramInput.addTextChangedListener(watcher);
        platinumSellKgInput.addTextChangedListener(watcher);
        silverBuyInput.addTextChangedListener(watcher);
        silverSellGramInput.addTextChangedListener(watcher);
        silverSellKgInput.addTextChangedListener(watcher);
    }

    private void scheduleSync() {
        handler.removeCallbacks(syncRunnable);
        handler.postDelayed(syncRunnable, 350);
    }

    private EditText priceInput(String value) {
        EditText input = new EditText(this);
        input.setText(value);
        input.setTextSize(20);
        input.setSingleLine(true);
        input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        input.setPadding(dp(12), dp(10), dp(12), dp(10));
        return input;
    }

    private TextView label(String text) {
        TextView label = new TextView(this);
        label.setText(text);
        label.setTextSize(15);
        label.setTypeface(Typeface.DEFAULT_BOLD);
        label.setTextColor(Color.rgb(114, 91, 74));
        label.setPadding(0, dp(10), 0, dp(4));
        return label;
    }

    private void readInputs() {
        prices.goldBuy = text(goldBuyInput);
        prices.goldSellGram = text(goldSellGramInput);
        prices.goldSellKg = text(goldSellKgInput);
        prices.goldSellAvailable = goldAvailable.isChecked();
        prices.platinumBuy = text(platinumBuyInput);
        prices.platinumSellGram = text(platinumSellGramInput);
        prices.platinumSellKg = text(platinumSellKgInput);
        prices.platinumSellAvailable = platinumAvailable.isChecked();
        prices.silverBuy = text(silverBuyInput);
        prices.silverSellGram = text(silverSellGramInput);
        prices.silverSellKg = text(silverSellKgInput);
        prices.silverSellAvailable = silverAvailable.isChecked();
    }

    private String text(EditText input) {
        return input.getText().toString().trim();
    }

    private void showDisplayMode() {
        displayMode = true;
        startPriceListener();

        FrameLayout root = new FrameLayout(this);
        ImageView bg = new ImageView(this);
        bg.setImageResource(getResources().getIdentifier("background", "drawable", getPackageName()));
        bg.setScaleType(ImageView.ScaleType.CENTER_CROP);
        root.addView(bg, new FrameLayout.LayoutParams(-1, -1));

        LinearLayout poster = new LinearLayout(this);
        poster.setOrientation(LinearLayout.VERTICAL);
        poster.setGravity(Gravity.CENTER_HORIZONTAL);
        poster.setPadding(dp(48), dp(6), dp(48), dp(58));
        root.addView(poster, new FrameLayout.LayoutParams(-1, -1));

        ImageView logo = new ImageView(this);
        logo.setImageResource(getResources().getIdentifier("logo", "drawable", getPackageName()));
        logo.setAdjustViewBounds(true);
        poster.addView(logo, new LinearLayout.LayoutParams(dp(190), dp(92)));

        TextView title = heading("DAILY GOLD PRICE", 34);
        title.setTypeface(Typeface.create(Typeface.SERIF, Typeface.BOLD));
        title.setGravity(Gravity.CENTER);
        poster.addView(title);
        TextView chineseTitle = heading("今日金价", 29);
        chineseTitle.setGravity(Gravity.CENTER);
        poster.addView(chineseTitle);

        LinearLayout dateBox = new LinearLayout(this);
        dateBox.setOrientation(LinearLayout.VERTICAL);
        dateBox.setGravity(Gravity.CENTER);
        dateBox.setPadding(dp(24), dp(5), dp(24), dp(5));
        dateBox.setBackgroundColor(Color.argb(150, 255, 255, 250));
        LinearLayout.LayoutParams dateParams = new LinearLayout.LayoutParams(-2, -2);
        dateParams.setMargins(0, dp(6), 0, dp(10));
        poster.addView(dateBox, dateParams);

        dateText = heading("", 19);
        dateText.setGravity(Gravity.CENTER);
        timeText = heading("", 12);
        timeText.setGravity(Gravity.CENTER);
        dateBox.addView(dateText);
        dateBox.addView(timeText);

        poster.addView(priceTable(), new LinearLayout.LayoutParams(-1, 0, 1f));

        TextView note = new TextView(this);
        note.setText("Prices are subject to change without prior notice. Thank you for your trust and support!\n价格如有变动，恕不另行通知。感谢您的信任与支持！");
        note.setTextColor(Color.rgb(92, 64, 51));
        note.setTextSize(14);
        note.setGravity(Gravity.CENTER);
        poster.addView(note);

        footer(root);
        goldBar(root);
        setContentView(root);
        renderPrices();
    }

    private TableLayout priceTable() {
        TableLayout table = new TableLayout(this);
        table.setStretchAllColumns(true);
        table.setShrinkAllColumns(true);
        table.setBackgroundColor(Color.argb(145, 255, 255, 250));

        TableRow header = new TableRow(this);
        header.setBackgroundColor(Color.rgb(232, 159, 12));
        header.addView(headerCell("TYPE/种类"));
        header.addView(headerCell("BUY-IN/回收价"));
        header.addView(headerCell("SELL/Gram\n售价/克"));
        header.addView(headerCell("SELL/KG\n售价/公斤"));
        table.addView(header, new TableLayout.LayoutParams(-1, dp(48)));

        addProductRow(table, "Au99.99\n黄金", 0);
        addProductRow(table, "PT999\n铂金999", 1);
        addProductRow(table, "Silver\n白银", 2);
        return table;
    }

    private void addProductRow(TableLayout table, String name, int index) {
        TableRow row = new TableRow(this);
        row.setBackgroundColor(Color.argb(145, 255, 255, 255));
        row.addView(productCell(name));
        TextView buy = priceCell("/Gram--/克");
        TextView sellGram = priceCell("/Gram--/克");
        TextView sellKg = priceCell("/KG -- /公斤");
        row.addView(buy);
        row.addView(sellGram);
        row.addView(sellKg);
        table.addView(row, new TableLayout.LayoutParams(-1, 0, 1f));

        if (index == 0) {
            goldBuy = buy;
            goldSellGram = sellGram;
            goldSellKg = sellKg;
        } else if (index == 1) {
            platinumBuy = buy;
            platinumSellGram = sellGram;
            platinumSellKg = sellKg;
        } else {
            silverBuy = buy;
            silverSellGram = sellGram;
            silverSellKg = sellKg;
        }
    }

    private TextView headerCell(String text) {
        TextView cell = tableText(text, 17, true);
        cell.setTextColor(Color.rgb(123, 51, 6));
        return cell;
    }

    private TextView productCell(String text) {
        return tableText(text, 18, false);
    }

    private TextView priceCell(String unit) {
        return tableText("-\n" + unit, 16, false);
    }

    private TextView tableText(String text, int sp, boolean bold) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextSize(sp);
        view.setTextColor(Color.rgb(92, 64, 51));
        view.setGravity(Gravity.CENTER);
        view.setPadding(dp(4), dp(3), dp(4), dp(3));
        if (bold) view.setTypeface(Typeface.DEFAULT_BOLD);
        view.setBackgroundColor(Color.TRANSPARENT);
        view.setLayoutParams(new TableRow.LayoutParams(0, -1, 1f));
        return view;
    }

    private void renderPrices() {
        updateDate();
        setValue(goldBuy, prices.goldBuy, true, "/Gram--/克");
        setValue(goldSellGram, prices.goldSellGram, prices.goldSellAvailable, "/Gram--/克");
        setValue(goldSellKg, prices.goldSellKg, prices.goldSellAvailable, "/KG -- /公斤");
        setValue(platinumBuy, prices.platinumBuy, true, "/Gram--/克");
        setValue(platinumSellGram, prices.platinumSellGram, prices.platinumSellAvailable, "/Gram--/克");
        setValue(platinumSellKg, prices.platinumSellKg, prices.platinumSellAvailable, "/KG -- /公斤");
        setValue(silverBuy, prices.silverBuy, true, "/Gram--/克");
        setValue(silverSellGram, prices.silverSellGram, prices.silverSellAvailable, "/Gram--/克");
        setValue(silverSellKg, prices.silverSellKg, prices.silverSellAvailable, "/KG -- /公斤");
    }

    private void setValue(TextView view, String raw, boolean available, String unit) {
        String formatted = available ? formatCurrency(raw) : "ENQUIRE / 请咨询";
        view.setText(formatted.equals("-") ? "ENQUIRE / 请咨询" : formatted + "\n" + unit);
        view.setTextSize(formatted.startsWith("ENQUIRE") ? 13 : 17);
        view.setTextColor(formatted.startsWith("ENQUIRE") ? Color.rgb(138, 101, 49) : Color.rgb(92, 64, 51));
        view.setTypeface(Typeface.DEFAULT_BOLD);
    }

    private String formatCurrency(String raw) {
        try {
            double value = Double.parseDouble(raw);
            if (value <= 0) return "-";
            NumberFormat formatter = NumberFormat.getCurrencyInstance(Locale.US);
            formatter.setMinimumFractionDigits(2);
            formatter.setMaximumFractionDigits(2);
            return formatter.format(value);
        } catch (Exception e) {
            return "-";
        }
    }

    private void updateDate() {
        Date now = new Date();
        dateText.setText("DATE: " + new SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH).format(now) +
                " / 日期: " + new SimpleDateFormat("yyyy年M月d日", Locale.CHINESE).format(now));
        timeText.setText("LAST UPDATED: " + new SimpleDateFormat("HH:mm", Locale.ENGLISH).format(now) +
                " / 更新时间: " + new SimpleDateFormat("HH:mm", Locale.ENGLISH).format(now));
    }

    private void footer(FrameLayout root) {
        LinearLayout footer = new LinearLayout(this);
        footer.setGravity(Gravity.CENTER);
        footer.setOrientation(LinearLayout.HORIZONTAL);
        footer.setPadding(dp(10), 0, dp(10), 0);
        footer.setBackgroundColor(Color.rgb(83, 55, 18));
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(-1, dp(54), Gravity.BOTTOM);
        root.addView(footer, params);
        footer.addView(footerItem("location", "PHNOM PENH, CAMBODIA"));
        footer.addView(footerItem("telegram", "Telegram: 069 793 168"));
        footer.addView(footerItem("phone", "Phone: 069 793 168"));
    }

    private LinearLayout footerItem(String image, String text) {
        LinearLayout item = new LinearLayout(this);
        item.setGravity(Gravity.CENTER);
        item.setOrientation(LinearLayout.HORIZONTAL);
        item.setPadding(dp(12), 0, dp(12), 0);
        ImageView icon = new ImageView(this);
        icon.setImageResource(getResources().getIdentifier(image, "drawable", getPackageName()));
        item.addView(icon, new LinearLayout.LayoutParams(dp(24), dp(24)));
        TextView label = new TextView(this);
        label.setText(text);
        label.setTextColor(Color.WHITE);
        label.setTextSize(15);
        label.setPadding(dp(8), 0, 0, 0);
        item.addView(label);
        return item;
    }

    private void goldBar(FrameLayout root) {
        ImageView gold = new ImageView(this);
        gold.setImageResource(getResources().getIdentifier("goldbar", "drawable", getPackageName()));
        gold.setAdjustViewBounds(true);
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(dp(145), dp(112), Gravity.BOTTOM | Gravity.RIGHT);
        params.setMargins(0, 0, dp(18), dp(42));
        root.addView(gold, params);
    }

    private TextView heading(String text, int sp) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextSize(sp);
        view.setTextColor(Color.rgb(92, 64, 51));
        view.setTypeface(Typeface.DEFAULT_BOLD);
        return view;
    }

    private void broadcastPrices() {
        new Thread(() -> {
            try {
                JSONObject json = prices.toJson();
                byte[] payload = json.toString().getBytes(StandardCharsets.UTF_8);
                DatagramSocket socket = new DatagramSocket();
                socket.setBroadcast(true);
                DatagramPacket packet = new DatagramPacket(payload, payload.length, InetAddress.getByName("255.255.255.255"), SYNC_PORT);
                for (int i = 0; i < 3; i++) {
                    socket.send(packet);
                    Thread.sleep(120);
                }
                socket.close();
            } catch (Exception ignored) {
            }
        }).start();
    }

    private void startPriceListener() {
        if (listening) return;
        listening = true;
        WifiManager wifi = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        multicastLock = wifi.createMulticastLock("gold-poster-sync");
        multicastLock.setReferenceCounted(true);
        multicastLock.acquire();

        new Thread(() -> {
            try {
                DatagramSocket socket = new DatagramSocket(SYNC_PORT);
                byte[] buffer = new byte[8192];
                while (listening) {
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                    socket.receive(packet);
                    String text = new String(packet.getData(), 0, packet.getLength(), StandardCharsets.UTF_8);
                    PriceData incoming = PriceData.fromJson(new JSONObject(text));
                    handler.post(() -> {
                        prices = incoming;
                        prices.save(prefs);
                        if (displayMode) renderPrices();
                    });
                }
                socket.close();
            } catch (Exception ignored) {
            }
        }).start();
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    public static class PriceData {
        String goldBuy = "";
        String goldSellGram = "";
        String goldSellKg = "";
        boolean goldSellAvailable = true;
        String platinumBuy = "";
        String platinumSellGram = "";
        String platinumSellKg = "";
        boolean platinumSellAvailable = false;
        String silverBuy = "";
        String silverSellGram = "";
        String silverSellKg = "";
        boolean silverSellAvailable = false;

        static PriceData fromPrefs(SharedPreferences prefs) {
            PriceData data = new PriceData();
            data.goldBuy = prefs.getString("goldBuy", "");
            data.goldSellGram = prefs.getString("goldSellGram", "");
            data.goldSellKg = prefs.getString("goldSellKg", "");
            data.goldSellAvailable = prefs.getBoolean("goldSellAvailable", true);
            data.platinumBuy = prefs.getString("platinumBuy", "");
            data.platinumSellGram = prefs.getString("platinumSellGram", "");
            data.platinumSellKg = prefs.getString("platinumSellKg", "");
            data.platinumSellAvailable = prefs.getBoolean("platinumSellAvailable", false);
            data.silverBuy = prefs.getString("silverBuy", "");
            data.silverSellGram = prefs.getString("silverSellGram", "");
            data.silverSellKg = prefs.getString("silverSellKg", "");
            data.silverSellAvailable = prefs.getBoolean("silverSellAvailable", false);
            return data;
        }

        static PriceData fromJson(JSONObject json) {
            PriceData data = new PriceData();
            data.goldBuy = json.optString("goldBuy", "");
            data.goldSellGram = json.optString("goldSellGram", "");
            data.goldSellKg = json.optString("goldSellKg", "");
            data.goldSellAvailable = json.optBoolean("goldSellAvailable", true);
            data.platinumBuy = json.optString("platinumBuy", "");
            data.platinumSellGram = json.optString("platinumSellGram", "");
            data.platinumSellKg = json.optString("platinumSellKg", "");
            data.platinumSellAvailable = json.optBoolean("platinumSellAvailable", false);
            data.silverBuy = json.optString("silverBuy", "");
            data.silverSellGram = json.optString("silverSellGram", "");
            data.silverSellKg = json.optString("silverSellKg", "");
            data.silverSellAvailable = json.optBoolean("silverSellAvailable", false);
            return data;
        }

        JSONObject toJson() throws Exception {
            JSONObject json = new JSONObject();
            json.put("goldBuy", goldBuy);
            json.put("goldSellGram", goldSellGram);
            json.put("goldSellKg", goldSellKg);
            json.put("goldSellAvailable", goldSellAvailable);
            json.put("platinumBuy", platinumBuy);
            json.put("platinumSellGram", platinumSellGram);
            json.put("platinumSellKg", platinumSellKg);
            json.put("platinumSellAvailable", platinumSellAvailable);
            json.put("silverBuy", silverBuy);
            json.put("silverSellGram", silverSellGram);
            json.put("silverSellKg", silverSellKg);
            json.put("silverSellAvailable", silverSellAvailable);
            return json;
        }

        void save(SharedPreferences prefs) {
            prefs.edit()
                    .putString("goldBuy", goldBuy)
                    .putString("goldSellGram", goldSellGram)
                    .putString("goldSellKg", goldSellKg)
                    .putBoolean("goldSellAvailable", goldSellAvailable)
                    .putString("platinumBuy", platinumBuy)
                    .putString("platinumSellGram", platinumSellGram)
                    .putString("platinumSellKg", platinumSellKg)
                    .putBoolean("platinumSellAvailable", platinumSellAvailable)
                    .putString("silverBuy", silverBuy)
                    .putString("silverSellGram", silverSellGram)
                    .putString("silverSellKg", silverSellKg)
                    .putBoolean("silverSellAvailable", silverSellAvailable)
                    .apply();
        }
    }
}

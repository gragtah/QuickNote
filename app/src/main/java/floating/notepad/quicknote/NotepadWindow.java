package floating.notepad.quicknote;

import android.app.Notification;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;

import java.util.ArrayList;
import java.util.List;

import wei.mark.standout.StandOutWindow;
import wei.mark.standout.constants.StandOutFlags;
import wei.mark.standout.ui.Window;

public class NotepadWindow extends StandOutWindow {

    SharedPreferences prefs;

    public static int width = 500;
    public static int height = 500;
    public static int collapsed_width = 96;
    public static int collapsed_height = 96;
    public boolean collapsed = false;

    @Override
    public String getAppName() {
        return ApplicationWrapper.getInstance().getAppName();
    }

    @Override
    public int getAppIcon() {
        return ApplicationWrapper.getInstance().getAppIcon();
    }

    @Override
    public void createAndAttachView(final int id, final FrameLayout frame) {
        collapsed = false;

        prefs = ApplicationWrapper.getInstance().getSharedPrefs();

        width = prefs.getInt(Constants.WIDTH_PREF, width);
        height = prefs.getInt(Constants.HEIGHT_PREF, height);
        collapsed_width = prefs.getInt(Constants.SMALL_WIDTH_PREF, collapsed_width);
        collapsed_height = prefs.getInt(Constants.SMALL_HEIGHT_PREF, collapsed_height);

        LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.notepad_layout, frame, true);
        EditText et = (EditText) frame.findViewById(R.id.editText);

        et.setText(prefs.getString(Constants.NOTE_CONTENT, ""));

        et.addTextChangedListener(new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                save(frame, id);
            }

            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void afterTextChanged(Editable s) { }
        });

        ImageButton dock = (ImageButton) frame.findViewById(R.id.dockButton);
        dock.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                collapsed = true;
                save(frame, id);
                if (prefs.contains(Constants.POS_X))
                    prefs.edit().putInt(Constants.POS_X, prefs.getInt(Constants.POS_X, -1) + width - collapsed_width).commit();
                frame.findViewById(R.id.editText).setVisibility(View.GONE);
                frame.findViewById(R.id.dockButton).setVisibility(View.GONE);
                frame.findViewById(R.id.settingsButton).setVisibility(View.GONE);
                frame.findViewById(R.id.openButton).setVisibility(View.VISIBLE);
                unfocus(id);
                updateViewLayout(id, getParams(id, null));
            }
        });

        ImageButton undock = (ImageButton) frame.findViewById(R.id.openButton);
        undock.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                collapsed = false;
                save(frame, id);
                if (prefs.contains(Constants.POS_X))
                    prefs.edit().putInt(Constants.POS_X, prefs.getInt(Constants.POS_X, -1) + collapsed_width - width).commit();
                frame.findViewById(R.id.editText).setVisibility(View.VISIBLE);
                frame.findViewById(R.id.dockButton).setVisibility(View.VISIBLE);
                frame.findViewById(R.id.settingsButton).setVisibility(View.VISIBLE);
                frame.findViewById(R.id.openButton).setVisibility(View.GONE);
                unfocus(id);
                updateViewLayout(id, getParams(id, null));
            }
        });

        ImageButton menu = (ImageButton) frame.findViewById(R.id.settingsButton);
        menu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getDropDown(id).showAsDropDown(v);
            }
        });

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                unfocus(id);
                updateViewLayout(id, getParams(id, null));
            }
        }, 10);
    }

    @Override
    public StandOutLayoutParams getParams(int id, Window window) {
        return new StandOutLayoutParams(id,
                collapsed ? collapsed_width : width,
                collapsed ? collapsed_height : height,
                prefs != null ? prefs.getInt(Constants.POS_X, StandOutLayoutParams.RIGHT) : StandOutLayoutParams.RIGHT,
                prefs != null ? prefs.getInt(Constants.POS_Y, StandOutLayoutParams.TOP) : StandOutLayoutParams.TOP);
    }

    @Override
    public int getFlags(int id) {
        return StandOutFlags.FLAG_BODY_MOVE_ENABLE |
               StandOutFlags.FLAG_WINDOW_EDGE_LIMITS_ENABLE |
               StandOutFlags.FLAG_WINDOW_FOCUS_INDICATOR_DISABLE;
    }

    public void onMove(int id, Window window, View view, MotionEvent event) {
        save(window, id);
    }

    @Override
    public Notification getPersistentNotification(int id) {
        return ApplicationWrapper.getInstance().getPersistentNotification();
    }

    public void save(FrameLayout frame, int id) {
        SharedPreferences.Editor editor = prefs.edit();
        String text = ((EditText)frame.findViewById(R.id.editText)).getText().toString();
        editor.putString(Constants.NOTE_CONTENT, text);
        editor.putInt(Constants.POS_X, getWindow(id).getLayoutParams().x);
        editor.putInt(Constants.POS_Y, getWindow(id).getLayoutParams().y);
        editor.apply();
    }

    public List<DropDownListItem> getDropDownItems(final int id) {
        List<DropDownListItem> items = new ArrayList<DropDownListItem>();
        items.add(new DropDownListItem(R.drawable.ic_action_delete, "Clear", new Runnable() {
            @Override
            public void run() {
                EditText et = (EditText) NotepadWindow.this.getWindow(id).findViewById(R.id.editText);
                et.setText("");
            }
        }));
        items.add(new DropDownListItem(R.drawable.ic_action_copy, "Copy", new Runnable() {
            @Override
            public void run() {
                ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                clipboard.setPrimaryClip(ClipData.newPlainText("Note", prefs.getString(Constants.NOTE_CONTENT, "")));
            }
        }));
        items.add(new DropDownListItem(R.drawable.ic_action_paste, "Paste", new Runnable() {
            @Override
            public void run() {
                try {
                    ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                    EditText et = (EditText) NotepadWindow.this.getWindow(id).findViewById(R.id.editText);
                    et.setText(prefs.getString(Constants.NOTE_CONTENT, "") + clipboard.getPrimaryClip().getItemAt(0).getText());
                } catch (Exception e) {

                }
            }
        }));
        items.add(new DropDownListItem(R.drawable.ic_action_share, "Share", new Runnable() {
            @Override
            public void run() {
                Intent sharingIntent = new Intent(android.content.Intent.ACTION_SEND);
                sharingIntent.setType("text/plain");
                sharingIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, "Quick Note");
                sharingIntent.putExtra(android.content.Intent.EXTRA_TEXT, prefs.getString(Constants.NOTE_CONTENT, ""));
                startActivity(Intent.createChooser(sharingIntent, "Share Note").setFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            }
        }));
        items.add(new DropDownListItem(R.drawable.ic_action_overflow, "Preferences", new Runnable() {
            @Override
            public void run() {
                StandOutWindow.hide(ApplicationWrapper.getInstance(), NotepadWindow.class, 0);
                StandOutWindow.show(ApplicationWrapper.getInstance(), PreferencesPopup.class, 1);
            }
        }));
        items.add(new DropDownListItem(R.drawable.ic_action_cancel, "Save & Quit", new Runnable() {
            @Override
            public void run() {
                closeAll();
            }
        }));
        return items;
    }
}
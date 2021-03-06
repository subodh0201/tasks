package com.example.tasks;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.media.Image;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import com.example.tasks.adapters.MoveTaskRecyclerAdapter;
import com.example.tasks.adapters.RecyclerAdapterCST;
import com.example.tasks.adapters.RecyclerAdapterIST;
import com.example.tasks.dataStructure.SubTask;
import com.example.tasks.dataStructure.Task;
import com.example.tasks.interfaces.OnRecyclerItemClickListener;
import com.example.tasks.interfaces.OnSubTaskItemClickListener;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.snackbar.Snackbar;

import org.w3c.dom.Text;

import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.Calendar;
import java.util.Date;

import it.xabaras.android.recyclerview.swipedecorator.RecyclerViewSwipeDecorator;

public class TaskViewActivity extends AppCompatActivity implements OnSubTaskItemClickListener {

    private TextView listTitleTV;
    private ImageButton changeStatusButton;
    private EditText taskTitleET;
    private EditText taskDetailET;
    private TextView completeHeader;
    private TextView taskDueDateTV;
    private ImageButton taskDueDateBtn;

    private RecyclerView incompleteSubTasksRecyclerView;
    RecyclerAdapterIST recyclerAdapterIST;
    private RecyclerView completeSubTaskRecyclerView;
    RecyclerAdapterCST recyclerAdapterCST;

    private DatePickerDialog datePickerDialog;
    int setYear = 0, setMonth = 0, setDay = 0;
    int setHour = 0, setMin = 0;
    boolean isTimeSet = false, isDateSet = false;

    private Task task;
    private Manager manager;

    private AlertDialog alertDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_task_view);

        Intent intent = getIntent();
        manager = new Manager(this);
        final int position = intent.getIntExtra("task_position", 0);
        boolean complete = intent.getBooleanExtra("task_status", false);
        if (complete && manager.getOpenList().completeTaskCount() <= position) {
            makeToast("Error while loading Task View due to wrong extra passed!");
            startListActivity();
        }

        if (complete)
            task = manager.getOpenList().getCompletedTask(position);
        else
            task = manager.getOpenList().getIncompleteTask(position);

        // Setting stuff up
        listTitleTV = (TextView) findViewById(R.id.list_title_tv_task_view);

        completeHeader = (TextView) findViewById(R.id.task_view_complete_sub_task_header);

        taskTitleET = (EditText) findViewById(R.id.task_title_edit_text);
        taskTitleET.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                manager.changeTaskTitle(task, taskTitleET.getText().toString());
            }
        });

        taskDetailET = (EditText) findViewById(R.id.task_details_edit_text);
        taskDetailET.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                manager.changeTaskDetails(task, taskDetailET.getText().toString());
            }
        });

        // Due date :
        taskDueDateTV = (TextView) findViewById(R.id.task_due_date_text_view);
        taskDueDateBtn = (ImageButton) findViewById(R.id.task_date_image_btn);
        taskDueDateBtn.setOnClickListener(v -> {setDueDate();});
        ConstraintLayout taskDueDateLayout = (ConstraintLayout) findViewById(R.id.task_due_date_constraint_layout);
        taskDueDateLayout.setOnClickListener(v -> {setDueDate();});

        // Back button
        ImageButton backButton = (ImageButton) findViewById(R.id.back_image_button_task_view);
        backButton.setOnClickListener(v -> onBackPressed());

        // delete button
        ImageButton deleteButton = (ImageButton) findViewById(R.id.delete_task_image_button_task_view);
        deleteButton.setOnClickListener(v -> deleteTask());

        // edit list button : changes the list in which the task belongs
        ImageButton editListButton = (ImageButton) findViewById(R.id.edit_list_button_task_view);
        editListButton.setOnClickListener(v -> changeList());

        // change status button : toggles complete / incomplete
        changeStatusButton = (ImageButton) findViewById(R.id.complete_image_button_task_view);
        changeStatusButton.setOnClickListener(v -> changeStatus());

        Button addNewSubTaskButton = (Button) findViewById(R.id.task_view_add_new_sub_task_button);
        addNewSubTaskButton.setOnClickListener(v -> showAddNewSubTaskDialog());

        // recycler views
        incompleteSubTasksRecyclerView = (RecyclerView) findViewById(R.id.sub_tasks_incomplete_recycler_view);
        incompleteSubTasksRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        ItemTouchHelper itemTouchHelperI = new ItemTouchHelper(simpleCallbackI);
        itemTouchHelperI.attachToRecyclerView(incompleteSubTasksRecyclerView);

        completeSubTaskRecyclerView = (RecyclerView) findViewById(R.id.sub_tasks_complete_recycler_view);
        completeSubTaskRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        ItemTouchHelper itemTouchHelperC = new ItemTouchHelper(simpleCallbackC);
        itemTouchHelperC.attachToRecyclerView(completeSubTaskRecyclerView);


        // setting up view for the first time
        setView();
    }

    private void setView() {
        listTitleTV.setText(manager.getListTitle());
        if (task.isComplete()) {
            changeStatusButton.setImageResource(R.drawable.ic_baseline_radio_button_unchecked_24);
            taskTitleET.setPaintFlags(taskTitleET.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
        } else {
            changeStatusButton.setImageResource(R.drawable.ic_baseline_check_24);
            taskTitleET.setPaintFlags(taskTitleET.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
        }
        taskTitleET.setText(task.getTitle());
        taskDetailET.setText(task.getDetails());
        taskDueDateTV.setText(task.getFormattedDueDateTime());
        if (task.isInPast()) taskDueDateTV.setTextColor(this.getResources().getColor(R.color.colorRedText));
        else taskDueDateTV.setTextColor(this.getResources().getColor(R.color.colorBlueText));

        recyclerAdapterIST = new RecyclerAdapterIST(task, this);
        incompleteSubTasksRecyclerView.setAdapter(recyclerAdapterIST);

        recyclerAdapterCST = new RecyclerAdapterCST(task, this);
        completeSubTaskRecyclerView.setAdapter(recyclerAdapterCST);
    }

    private void setDueDate() {
        final AlertDialog.Builder alert = new AlertDialog.Builder(TaskViewActivity.this);
        View mView = getLayoutInflater().inflate(R.layout.task_set_date_dialog, null);

        Button cancelBtn = (Button) mView.findViewById(R.id.cancel_task_date_btn);
        Button saveBtn = (Button) mView.findViewById(R.id.save_task_date_btn);
        ImageButton setDateBtn = (ImageButton) mView.findViewById(R.id.task_set_date_btn);
        ImageButton setTimeBtn = (ImageButton) mView.findViewById(R.id.task_set_time_btn);
        TextView dateTV = (TextView) mView.findViewById(R.id.task_date_text_view);
        TextView timeTV = (TextView) mView.findViewById(R.id.task_time_text_view);
        ImageButton removeDateBtn = (ImageButton) mView.findViewById(R.id.task_remove_date_btn);
        ImageButton removeTimeBtn = (ImageButton) mView.findViewById(R.id.task_remove_time_btn);

        // initial set up :
        dateTV.setText(task.getFormattedDue_Date());
        timeTV.setText(task.getFormattedDue_Time());
        if (task.getDueDateTime() == null) {
            removeDateBtn.setVisibility(View.GONE);
            timeTV.setVisibility(View.GONE);
            setTimeBtn.setVisibility(View.GONE);
            removeTimeBtn.setVisibility(View.GONE);
            setYear = setDay = setMonth = setHour = setMin = 0;
            isDateSet = isTimeSet = false;
        } else {
            setYear = task.getDueDateTime().getYear();
            setMonth = task.getDueDateTime().getMonthValue() - 1;
            setDay = task.getDueDateTime().getDayOfMonth();
            isDateSet = true;
            setHour = setMin = 0;
            isTimeSet = false;
        }
        if (!task.isTimeSet()) {
            removeTimeBtn.setVisibility(View.GONE);
            setHour = setMin = 0;
            isTimeSet = false;
        } else {
            setHour = task.getDueDateTime().getHour();
            setMin = task.getDueDateTime().getMinute();
            isTimeSet = true;
        }

        removeDateBtn.setOnClickListener(v -> {
            dateTV.setText("");
            timeTV.setText("");
            setTimeBtn.setVisibility(View.GONE);
            timeTV.setVisibility(View.GONE);
            removeDateBtn.setVisibility(View.GONE);
            removeTimeBtn.setVisibility(View.GONE);
            setYear = setDay = setMonth = setHour = setMin = 0;
            isDateSet = isTimeSet = false;
        });

        removeTimeBtn.setOnClickListener(v -> {
            timeTV.setText("");
            removeTimeBtn.setVisibility(View.GONE);
            setHour = setMin = 0;
            isTimeSet = false;
        });

        setDateBtn.setOnClickListener(v -> {
            final Calendar cldr = Calendar.getInstance();
            int mday, mmonth, myear;
            if (isDateSet) {
                mday = setDay;
                mmonth = setMonth;
                myear = setYear;
            } else if (task.getDueDateTime() != null) {
                mday = task.getDueDateTime().getDayOfMonth();
                mmonth = task.getDueDateTime().getMonthValue() - 1;
                myear = task.getDueDateTime().getYear();
            } else {
                mday = cldr.get(Calendar.DAY_OF_MONTH);
                mmonth = cldr.get(Calendar.MONTH);
                myear = cldr.get(Calendar.YEAR);
            }

            // date picker dialog
            datePickerDialog = new DatePickerDialog(TaskViewActivity.this, R.style.MyDatePickerStyle ,
                    new DatePickerDialog.OnDateSetListener() {
                        @Override
                        public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                            setYear = year;
                            setDay = dayOfMonth;
                            setMonth = monthOfYear;
                            isDateSet = true;
                            SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, dd MMM yyyy");
                            dateTV.setText(dateFormat.format(new Date(setYear - 1900, setMonth, setDay, setHour, setMin)));
                            setTimeBtn.setVisibility(View.VISIBLE);
                            removeDateBtn.setVisibility(View.VISIBLE);
                            timeTV.setVisibility(View.VISIBLE);
                        }
                    }, myear, mmonth, mday);
            datePickerDialog.show();
        });

        setTimeBtn.setOnClickListener(v -> {
            final Calendar c = Calendar.getInstance();
            int mHour, mMinute;
            if (isTimeSet) {
                mHour = setHour;
                mMinute = setMin;
            } else if (task.isTimeSet()) {
                mHour = task.getDueDateTime().getHour();
                mMinute = task.getDueDateTime().getMinute();
            } else {
                mHour = c.get(Calendar.HOUR_OF_DAY);
                mMinute = c.get(Calendar.MINUTE);
            }

            // Launch Time Picker Dialog
            TimePickerDialog timePickerDialog = new TimePickerDialog(TaskViewActivity.this,
                    new TimePickerDialog.OnTimeSetListener() {
                        @Override
                        public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                            setHour = hourOfDay;
                            setMin = minute;
                            isTimeSet = true;
                            SimpleDateFormat dateFormat = new SimpleDateFormat("HH : mm");
                            timeTV.setText(dateFormat.format(new Date(setYear - 1900, setMonth, setDay, setHour, setMin)));
                            removeTimeBtn.setVisibility(View.VISIBLE);
                        }
                    }, mHour, mMinute, false);
            timePickerDialog.show();
        });

        cancelBtn.setOnClickListener(v -> {
            alertDialog.dismiss();
        });

        saveBtn.setOnClickListener(v -> {
            LocalDateTime taskDateTime = null;
            if (isDateSet) taskDateTime = LocalDateTime.of(setYear, setMonth + 1, setDay, setHour, setMin);
            task.setDueDateTime(taskDateTime);
            task.setTimeSet(isTimeSet);
            alertDialog.dismiss();
            setView();
        });
        mView.setBackgroundColor(getResources().getColor(R.color.colorSecondaryBg));
        alert.setView(mView);
        alertDialog = alert.create();
        alertDialog.setCanceledOnTouchOutside(true);
        alertDialog.show();
    }

    private void showAddNewSubTaskDialog() {
        final AlertDialog.Builder alert = new AlertDialog.Builder(TaskViewActivity.this);
        View mView = getLayoutInflater().inflate(R.layout.change_sub_task_title, null);

        final EditText taskTitleET = (EditText) mView.findViewById(R.id.change_sub_task_title_ET);
        Button addNewTasButton = (Button) mView.findViewById(R.id.change_sub_task_title_button);
        addNewTasButton.setText(R.string.Add);
        alert.setView(mView);

        TextView header = (TextView) mView.findViewById(R.id.Sub_task_title_cd_header);
        header.setText(R.string.AddNewSubTask);

        alertDialog = alert.create();
        alertDialog.setCanceledOnTouchOutside(true);

        taskTitleET.setOnFocusChangeListener((v, hasFocus) -> taskTitleET.post(() -> {
            InputMethodManager inputMethodManager = (InputMethodManager) TaskViewActivity.this.getSystemService(Context.INPUT_METHOD_SERVICE);
            inputMethodManager.showSoftInput(taskTitleET, InputMethodManager.SHOW_IMPLICIT);
        }));

        addNewTasButton.setOnClickListener(v -> {
            String title = taskTitleET.getText().toString().trim();
            if (title.equals("")) {
                makeToast("Provide a valid title");
                alertDialog.dismiss();
            } else {
                int insertedAt = manager.addSubTask(title, task);
                recyclerAdapterIST.notifyItemInserted(insertedAt);
                alertDialog.dismiss();
                makeToast("Sub task added");
            }
        });
        alertDialog.show();

        taskTitleET.requestFocus();
    }

    private void changeStatus() {
        manager.changeStatusOfTask(task);
        if (task.isComplete()) {
            Snackbar snackbar = Snackbar.make(incompleteSubTasksRecyclerView, "Task marked as complete", Snackbar.LENGTH_LONG);
            snackbar.setAnchorView(findViewById(R.id.task_view_bottom_bar));
            View sbView = snackbar.getView();
            sbView.setBackgroundColor(ContextCompat.getColor(this, R.color.colorSecondaryBg));
            snackbar.show();
        } else {
            Snackbar snackbar = Snackbar.make(incompleteSubTasksRecyclerView, "Task marked as incomplete", Snackbar.LENGTH_LONG);
            snackbar.setAnchorView(findViewById(R.id.task_view_bottom_bar));
            View sbView = snackbar.getView();
            sbView.setBackgroundColor(ContextCompat.getColor(this, R.color.colorSecondaryBg));
            snackbar.show();
        }
        setView();
    }

    private void changeList() {
        View dialogView = getLayoutInflater().inflate(R.layout.move_task_to_list_bottom_sheet, null);
        final BottomSheetDialog dialog = new BottomSheetDialog(this);
        dialog.setContentView(dialogView);
        OnRecyclerItemClickListener listener = position -> {
            manager.moveTaskToList(task, position);
            startListActivity();
            dialog.cancel();
            finish();
        };
        RecyclerView moveTaskRecyclerView = (RecyclerView) dialogView.findViewById(R.id.move_task_to_recycler_view);
        moveTaskRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        MoveTaskRecyclerAdapter moveTaskRecyclerAdapter = new MoveTaskRecyclerAdapter(manager.getUser(), listener, this);
        moveTaskRecyclerView.setAdapter(moveTaskRecyclerAdapter);
        dialog.show();
    }

    private void deleteTask() {
        final AlertDialog.Builder alert = new AlertDialog.Builder(TaskViewActivity.this);
        View mView = getLayoutInflater().inflate(R.layout.continue_or_cancle, null);

        final TextView messageTextView = (TextView) mView.findViewById(R.id.message_text_view);
        Button positiveButton = (Button) mView.findViewById(R.id.positive_button);
        Button negativeButton = (Button) mView.findViewById(R.id.negative_button);

        alert.setView(mView);
        String message = "The task will be deleted forever.";
        messageTextView.setText(message);

        alertDialog = alert.create();
        alertDialog.setCanceledOnTouchOutside(false);

        positiveButton.setText(R.string.Delete);
        positiveButton.setOnClickListener(v -> {
            manager.deleteTask(task);
            makeToast("Task deleted");
            alertDialog.cancel();
            startListActivity();
        });

        negativeButton.setText(R.string.Cancel);
        negativeButton.setOnClickListener(v -> alertDialog.cancel());
        alertDialog.show();
    }

    @Override
    public void onCompleteButtonClick(int position, boolean complete) {
        if (complete) incompleteSubTask(position);
        else completeSubTask(position);
        // notify item removed!
    }

    private void incompleteSubTask(final int position) {
        final SubTask subTask = task.getCompleteSubTaskAt(position);
        int insertedAt = manager.incompleteSubTaskOf(task, position);
        recyclerAdapterCST.notifyItemRemoved(position);
        recyclerAdapterIST.notifyItemInserted(insertedAt);
        Snackbar snackbar = Snackbar.make(completeSubTaskRecyclerView, R.string.SubTaskMarkedAsIncomplete, Snackbar.LENGTH_LONG);
        snackbar.setAnchorView(findViewById(R.id.task_view_bottom_bar));
        snackbar.setActionTextColor(ContextCompat.getColor(this, R.color.colorAccentSecondary));
        snackbar.setAction(R.string.Undo, v -> {
            int deletedFrom =  manager.undoIncompleteSubTaskOf(task, subTask, position);
            recyclerAdapterCST.notifyItemInserted(position);
            recyclerAdapterIST.notifyItemRemoved(deletedFrom);
        });
        View sbView = snackbar.getView();
        sbView.setBackgroundColor(ContextCompat.getColor(this, R.color.colorSecondaryBg));
        snackbar.show();
    }

    private void completeSubTask(final int position) {
        final SubTask subTask = task.getIncompleteSubTaskAt(position);
        int insertedAt = manager.completeSubTaskOf(task, position);
        recyclerAdapterIST.notifyItemRemoved(position);
        recyclerAdapterCST.notifyItemInserted(insertedAt);
        Snackbar snackbar = Snackbar.make(incompleteSubTasksRecyclerView, R.string.SubTaskMarkedAsComplete, Snackbar.LENGTH_LONG);
        snackbar.setAnchorView(findViewById(R.id.task_view_bottom_bar));
        snackbar.setActionTextColor(ContextCompat.getColor(this, R.color.colorAccentSecondary));
        snackbar.setAction(R.string.Undo, v -> {
            int deletedFrom = manager.undoCompleteSubTaskOf(task, subTask, position);
            recyclerAdapterIST.notifyItemInserted(position);
            recyclerAdapterCST.notifyItemRemoved(deletedFrom);
        });
        View sbView = snackbar.getView();
        sbView.setBackgroundColor(ContextCompat.getColor(this, R.color.colorSecondaryBg));
        snackbar.show();
    }

    @Override
    public void onDeleteButtonClick(int position, boolean complete) {
        if (complete) deleteCompleteSubTask(position);
        else deleteIncompleteSubTask(position);
    }

    private void deleteCompleteSubTask(final int position) {
        final SubTask subTask = task.getCompleteSubTaskAt(position);
        manager.deleteCompleteSubTask(task, position);
        recyclerAdapterCST.notifyItemRemoved(position);

        Snackbar snackbar = Snackbar.make(completeSubTaskRecyclerView, R.string.SubTaskDeleted, Snackbar.LENGTH_LONG);
        snackbar.setAnchorView(findViewById(R.id.task_view_bottom_bar));
        snackbar.setActionTextColor(ContextCompat.getColor(this, R.color.colorAccentSecondary));
        snackbar.setAction(R.string.Undo, v -> {
            manager.undoDeleteCompleteSubTask(task, subTask, position);
            recyclerAdapterCST.notifyItemInserted(position);
        });
        View sbView = snackbar.getView();
        sbView.setBackgroundColor(ContextCompat.getColor(this, R.color.colorSecondaryBg));
        snackbar.show();
    }

    private void deleteIncompleteSubTask(final int position) {
        final SubTask subTask = task.getIncompleteSubTaskAt(position);
        manager.deleteIncompleteSubTask(task, position);
        recyclerAdapterIST.notifyItemRemoved(position);

        Snackbar snackbar = Snackbar.make(incompleteSubTasksRecyclerView, R.string.SubTaskDeleted, Snackbar.LENGTH_LONG);
        snackbar.setAnchorView(findViewById(R.id.task_view_bottom_bar));
        snackbar.setActionTextColor(ContextCompat.getColor(this, R.color.colorAccentSecondary));
        snackbar.setAction(R.string.Undo, v -> {
            manager.undoDeleteIncompleteSubTask(task, subTask, position);
            recyclerAdapterIST.notifyItemInserted(position);
        });
        View sbView = snackbar.getView();
        sbView.setBackgroundColor(ContextCompat.getColor(this, R.color.colorSecondaryBg));
        snackbar.show();
    }

    @Override
    public void onItemClick(int position, boolean complete) {
        showSubTaskTitleChangeDialog(position, complete);
        // notify data changed!
    }

    private void showSubTaskTitleChangeDialog(final int position, final boolean complete) {
        final AlertDialog.Builder alert = new AlertDialog.Builder(TaskViewActivity.this);
        View mView = getLayoutInflater().inflate(R.layout.change_sub_task_title, null);
        final EditText taskTitleET = (EditText) mView.findViewById(R.id.change_sub_task_title_ET);
        if (complete) taskTitleET.setText(task.getCompleteSubTaskAt(position).getTitle());
        else taskTitleET.setText(task.getIncompleteSubTaskAt(position).getTitle());
        Button addNewTasButton = (Button) mView.findViewById(R.id.change_sub_task_title_button);

        TextView header = (TextView) mView.findViewById(R.id.Sub_task_title_cd_header);
        header.setText(R.string.ChangeSubTaskTitle);

        alert.setView(mView);

        alertDialog = alert.create();
        alertDialog.setCanceledOnTouchOutside(true);

        taskTitleET.setOnFocusChangeListener((v, hasFocus) -> taskTitleET.post(() -> {
            InputMethodManager inputMethodManager = (InputMethodManager) TaskViewActivity.this.getSystemService(Context.INPUT_METHOD_SERVICE);
            inputMethodManager.showSoftInput(taskTitleET, InputMethodManager.SHOW_IMPLICIT);
        }));

        addNewTasButton.setOnClickListener(v -> {
            String title = taskTitleET.getText().toString().trim();
            if (title.equals("")) {
                makeToast("Provide a valid title");
                alertDialog.dismiss();
            } else {
                if (complete) manager.changeCompleteSubTaskTitle(task, position, title);
                else manager.changeIncompleteSubTaskTitle(task, position, title);
                if (complete) recyclerAdapterCST.notifyDataSetChanged();
                else recyclerAdapterIST.notifyDataSetChanged();
                alertDialog.dismiss();
                makeToast("Title of sub task changed");
            }
        });

        alertDialog.show();

        taskTitleET.requestFocus();
    }

    @Override
    public void setCompleteHeader(boolean has) {
        if (has) completeHeader.setText(R.string.Complete);
        else completeHeader.setText("");
    }

    private void startListActivity() {
        onBackPressed();
        finish();
    }

    public void makeToast(String str) {
        Toast.makeText(this, str, Toast.LENGTH_SHORT).show();
    }

    ItemTouchHelper.SimpleCallback simpleCallbackI = new ItemTouchHelper.SimpleCallback((ItemTouchHelper.UP | ItemTouchHelper.DOWN), ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
        @Override
        public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
            // getting positions
            int fromPosition = viewHolder.getAdapterPosition();
            int toPosition = target.getAdapterPosition();

            // swapping in database
            manager.swapIncompleteSubTasks(task, fromPosition, toPosition);

            // notifying the adapter
            recyclerView.getAdapter().notifyItemMoved(fromPosition, toPosition);
            return true;
        }

        @Override
        public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
            int position = viewHolder.getAdapterPosition();
            switch (direction) {
                case ItemTouchHelper.LEFT:
                    deleteIncompleteSubTask(position);
                    break;
                case ItemTouchHelper.RIGHT:
                    completeSubTask(position);
                    break;
            }
        }

        @Override
        public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {
            new RecyclerViewSwipeDecorator.Builder(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
                    .addSwipeRightActionIcon(R.drawable.ic_baseline_done_24_accent)
                    .addSwipeLeftActionIcon(R.drawable.ic_baseline_delete_24_accent)
                    .addBackgroundColor(ContextCompat.getColor(TaskViewActivity.this, R.color.colorAccentPrimary))
                    .create()
                    .decorate();
            super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
        }

        @Override
        public void onSelectedChanged(@Nullable RecyclerView.ViewHolder viewHolder, int actionState) {
            super.onSelectedChanged(viewHolder, actionState);
            if (actionState == ItemTouchHelper.ACTION_STATE_DRAG) {
                viewHolder.itemView.setBackgroundColor(ContextCompat.getColor(TaskViewActivity.this, R.color.colorSecondaryBg));
            }
        }

        @Override
        public void clearView(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
            super.clearView(recyclerView, viewHolder);
            viewHolder.itemView.setBackgroundColor(ContextCompat.getColor(TaskViewActivity.this, R.color.colorPrimaryBg));
        }
    };

    ItemTouchHelper.SimpleCallback simpleCallbackC = new ItemTouchHelper.SimpleCallback((ItemTouchHelper.UP | ItemTouchHelper.DOWN), ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
        @Override
        public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
            // getting positions
            int fromPosition = viewHolder.getAdapterPosition();
            int toPosition = target.getAdapterPosition();
            // swapping in database
            manager.swapCompleteSubTasks(task, fromPosition, toPosition);

            // notifying the adapter
            recyclerView.getAdapter().notifyItemMoved(fromPosition, toPosition);
            return true;
        }

        @Override
        public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
            int position = viewHolder.getAdapterPosition();
            switch (direction) {
                case ItemTouchHelper.LEFT:
                    deleteCompleteSubTask(position);
                    break;
                case ItemTouchHelper.RIGHT:
                    incompleteSubTask(position);
                    break;
            }
        }

        @Override
        public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {
            new RecyclerViewSwipeDecorator.Builder(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
                    .addSwipeRightActionIcon(R.drawable.ic_baseline_radio_button_unchecked_24)
                    .addSwipeLeftActionIcon(R.drawable.ic_baseline_delete_24_accent)
                    .addBackgroundColor(ContextCompat.getColor(TaskViewActivity.this, R.color.colorAccentPrimary))
                    .create()
                    .decorate();
            super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
        }

        @Override
        public void onSelectedChanged(@Nullable RecyclerView.ViewHolder viewHolder, int actionState) {
            super.onSelectedChanged(viewHolder, actionState);
            if (actionState == ItemTouchHelper.ACTION_STATE_DRAG) {
                viewHolder.itemView.setBackgroundColor(ContextCompat.getColor(TaskViewActivity.this, R.color.colorSecondaryBg));
            }
        }

        @Override
        public void clearView(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
            super.clearView(recyclerView, viewHolder);
            viewHolder.itemView.setBackgroundColor(ContextCompat.getColor(TaskViewActivity.this, R.color.colorPrimaryBg));
        }
    };
}
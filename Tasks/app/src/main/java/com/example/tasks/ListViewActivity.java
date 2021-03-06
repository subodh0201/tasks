package com.example.tasks;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import com.example.tasks.adapters.RecyclerAdapterCT;
import com.example.tasks.adapters.RecyclerAdapterIT;
import com.example.tasks.dataStructure.Task;
import com.example.tasks.interfaces.TaskCardViewListener;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.Calendar;
import java.util.Date;

import it.xabaras.android.recyclerview.swipedecorator.RecyclerViewSwipeDecorator;

public class ListViewActivity extends AppCompatActivity implements TaskCardViewListener{

    private Manager manager;                // manager object
    private TextView listTitleTextView;     // title of the list!

    public TextView completedTaskListTitle; // weird stuff

    private RecyclerView recyclerViewI;     // for incomplete tasks
    private RecyclerAdapterIT myAdapterI;   // adapter for incomplete task

    private RecyclerView recyclerViewC;     // for complete tasks
    private RecyclerAdapterCT myAdapterC;
    private AlertDialog alertDialog;

    FloatingActionButton addTaskFAB;

    private DatePickerDialog datePickerDialog;
    int setYear = 0, setMonth = 0, setDay = 0;
    int setHour = 0, setMinute = 0;
    boolean dateSet = false;
    boolean timeSet = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list_view);

        // Setting up stuff :
        manager = new Manager(this);
        listTitleTextView = (TextView) findViewById(R.id.list_title_tv);
        completedTaskListTitle = (TextView) findViewById(R.id.completed_task_list_title);

        // Bottom tray :
        //  Show all list image button
        ImageButton showAllLists = (ImageButton) findViewById(R.id.show_lists_button);
        showAllLists.setOnClickListener(v -> startShowLists());

        //  show options image button
        ImageButton optionsButton = (ImageButton) findViewById(R.id.list_view_options);
        optionsButton.setOnClickListener(v -> showOptions());

        // Recycler views
        //  Recycler view for incomplete tasks
        recyclerViewI = (RecyclerView) findViewById(R.id.tasks_recycler_view);
        recyclerViewI.setLayoutManager(new LinearLayoutManager(this));
        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(simpleCallback);
        itemTouchHelper.attachToRecyclerView(recyclerViewI);

        //  Recycler view for completed tasks
        recyclerViewC = (RecyclerView) findViewById(R.id.tasks_recycler_view_complete);
        recyclerViewC.setLayoutManager(new LinearLayoutManager(this));
        ItemTouchHelper itemTouchHelperComplete = new ItemTouchHelper(simpleCallbackComplete);
        itemTouchHelperComplete.attachToRecyclerView(recyclerViewC);

        // Floating button to add new task
        addTaskFAB = (FloatingActionButton) findViewById(R.id.add_task_fab);
        addTaskFAB.setOnClickListener(v -> showAddTaskDialog());

        // set up the list view for the first time
        setListView();
    }

    // update the View
    private void setListView() {
        // Set title of list in view
        listTitleTextView.setText(manager.getListTitle());

        // for incomplete tasks
        myAdapterI = new RecyclerAdapterIT(manager.getOpenList(), this, this, manager, addTaskFAB);
        recyclerViewI.setAdapter(myAdapterI);

        // for complete tasks
        myAdapterC = new RecyclerAdapterCT(manager.getOpenList(), this);
        recyclerViewC.setAdapter(myAdapterC);
    }


    // Methods related to task:
    // Add new Task :
    private void showAddTaskDialog() {
        final AlertDialog.Builder alert = new AlertDialog.Builder(ListViewActivity.this);
        View mView = getLayoutInflater().inflate(R.layout.new_task_cd, null);

        final EditText taskTitleET = (EditText) mView.findViewById(R.id.new_task_title);
        final EditText taskDetailET = (EditText) mView.findViewById(R.id.new_task_details);
        Button addNewTaskButton = (Button) mView.findViewById(R.id.add_new_task_button);
        ImageButton setTaskDate = (ImageButton) mView.findViewById(R.id.new_task_set_date_btn);
        ImageButton setTaskTime = (ImageButton) mView.findViewById(R.id.new_task_set_time_btn);
        TextView newTaskDateTV = (TextView) mView.findViewById(R.id.new_task_date_text_view);
        TextView newTaskTimeTV = (TextView) mView.findViewById(R.id.new_task_time_text_view);
        ImageButton removeDate = (ImageButton) mView.findViewById(R.id.new_task_remove_date_btn);
        ImageButton removeTime = (ImageButton) mView.findViewById(R.id.new_task_remove_time_btn);
        setTaskTime.setVisibility(View.GONE);
        newTaskTimeTV.setVisibility(View.GONE);
        removeDate.setVisibility(View.GONE);
        removeTime.setVisibility(View.GONE);
        removeDate.setOnClickListener(v -> {
            newTaskDateTV.setText("");
            newTaskTimeTV.setText("");
            setTaskTime.setVisibility(View.GONE);
            newTaskTimeTV.setVisibility(View.GONE);
            removeDate.setVisibility(View.GONE);
            removeTime.setVisibility(View.GONE);
            setYear = setDay = setMonth = setHour = setMinute = 0;
            dateSet = false;
        });
        removeTime.setOnClickListener(v -> {
            newTaskTimeTV.setText("");
            removeTime.setVisibility(View.GONE);
            setHour = setMinute = 0;
            timeSet = false;
        });

        setYear = setDay = setMonth = setHour = setMinute = 0;
        dateSet = timeSet = false;

        taskTitleET.setOnFocusChangeListener((v, hasFocus) -> taskTitleET.post(() -> {
            InputMethodManager inputMethodManager= (InputMethodManager) ListViewActivity.this.getSystemService(Context.INPUT_METHOD_SERVICE);
            inputMethodManager.showSoftInput(taskTitleET, InputMethodManager.SHOW_IMPLICIT);
        }));

        setTaskDate.setOnClickListener(v -> {
            final Calendar cldr = Calendar.getInstance();
            int mday = cldr.get(Calendar.DAY_OF_MONTH);
            int mmonth = cldr.get(Calendar.MONTH);
            int myear = cldr.get(Calendar.YEAR);
            // date picker dialog
            datePickerDialog = new DatePickerDialog(ListViewActivity.this, R.style.MyDatePickerStyle ,
                    new DatePickerDialog.OnDateSetListener() {
                        @Override
                        public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                            setYear = year;
                            setDay = dayOfMonth;
                            setMonth = monthOfYear;
                            dateSet = true;
                            SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, dd MMM yyyy");
                            newTaskDateTV.setText(dateFormat.format(new Date(setYear - 1900, setMonth, setDay, setHour, setMinute)));
                            setTaskTime.setVisibility(View.VISIBLE);
                            removeDate.setVisibility(View.VISIBLE);
                            newTaskTimeTV.setVisibility(View.VISIBLE);
                        }
                    }, myear, mmonth, mday);
            datePickerDialog.show();
        });

        setTaskTime.setOnClickListener(v -> {
            final Calendar c = Calendar.getInstance();
            int mHour = c.get(Calendar.HOUR_OF_DAY);
            int mMinute = c.get(Calendar.MINUTE);

            // Launch Time Picker Dialog
            TimePickerDialog timePickerDialog = new TimePickerDialog(ListViewActivity.this,
                    new TimePickerDialog.OnTimeSetListener() {
                        @Override
                        public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                            setHour = hourOfDay;
                            setMinute = minute;
                            timeSet = true;
                            SimpleDateFormat dateFormat = new SimpleDateFormat("HH : mm");
                            newTaskTimeTV.setText(dateFormat.format(new Date(setYear - 1900, setMonth, setDay, setHour, setMinute)));
                            removeTime.setVisibility(View.VISIBLE);
                        }
                    }, mHour, mMinute, false);
            timePickerDialog.show();
        });

        addNewTaskButton.setOnClickListener(v -> {
            String name = taskTitleET.getText().toString();
            String detail = taskDetailET.getText().toString();
            detail.trim();
            name = name.trim();
            if (name.equals("")) {
                makeToast("Provide a valid Task title");
            } else {
                LocalDateTime taskDateTime = null;
                if (dateSet) taskDateTime = LocalDateTime.of(setYear, setMonth + 1, setDay, setHour, setMinute);
                int addedAtIndex = manager.addTask(name, detail, taskDateTime, timeSet);
                alertDialog.dismiss();

                Snackbar snackbar = Snackbar.make(recyclerViewI, R.string.NewTaskAdded, Snackbar.LENGTH_LONG);
                snackbar.setAnchorView(addTaskFAB);
                View sbView = snackbar.getView();
                sbView.setBackgroundColor(ContextCompat.getColor(this, R.color.colorSecondaryBg));
                snackbar.show();
                myAdapterI.notifyItemInserted(addedAtIndex);
            }
        });

        alert.setView(mView);
        alertDialog = alert.create();
        alertDialog.setCanceledOnTouchOutside(true);
        alertDialog.show();
        taskTitleET.requestFocus();
    }

    // delete an incomplete task on swipe
    private void deleteIncompleteTask(final int position) {

        final AlertDialog.Builder alert = new AlertDialog.Builder(ListViewActivity.this);
        alert.setCancelable(false);
        View mView = getLayoutInflater().inflate(R.layout.continue_or_cancle, null);

        final TextView messageTextView = (TextView) mView.findViewById(R.id.message_text_view);
        Button positiveButton = (Button) mView.findViewById(R.id.positive_button);
        Button negativeButton = (Button) mView.findViewById(R.id.negative_button);

        alert.setView(mView);
        String message = "The incomplete task '" + manager.getIncompleteTaskTitle(position) + "' will be deleted permanently.";
        messageTextView.setText(message);

        alertDialog = alert.create();
        alertDialog.setCanceledOnTouchOutside(false);

        positiveButton.setText(R.string.Delete);
        positiveButton.setOnClickListener(v -> {
            manager.deleteIncompleteTask(position);
            myAdapterI.notifyItemRemoved(position);
            Snackbar snackbar = Snackbar.make(recyclerViewI, R.string.OneITDeleted, Snackbar.LENGTH_LONG);
            snackbar.setAnchorView(addTaskFAB);
            View sbView = snackbar.getView();
            sbView.setBackgroundColor(ContextCompat.getColor(this, R.color.colorSecondaryBg));
            snackbar.show();
            alertDialog.cancel();
        });

        negativeButton.setText(R.string.Cancel);
        negativeButton.setOnClickListener(v -> {
            alertDialog.cancel();
            myAdapterI.notifyItemChanged(position);
        });
        alertDialog.show();
    }

    // delete a complete task on swipe
    private void deleteCompleteTask(final int position) {
        final AlertDialog.Builder alert = new AlertDialog.Builder(ListViewActivity.this);
        alert.setCancelable(false);
        View mView = getLayoutInflater().inflate(R.layout.continue_or_cancle, null);

        final TextView messageTextView = (TextView) mView.findViewById(R.id.message_text_view);
        Button positiveButton = (Button) mView.findViewById(R.id.positive_button);
        Button negativeButton = (Button) mView.findViewById(R.id.negative_button);

        alert.setView(mView);
        String message = "The Task '" + manager.getCompleteTaskTitle(position) + "' will be deleted permanently.";
        messageTextView.setText(message);

        alertDialog = alert.create();
        alertDialog.setCanceledOnTouchOutside(false);

        positiveButton.setText(R.string.Delete);
        positiveButton.setOnClickListener(v -> {
            manager.deleteCompleteTask(position);
            Snackbar snackbar = Snackbar.make(recyclerViewI, R.string.OneCTDeleted, Snackbar.LENGTH_LONG);
            snackbar.setAnchorView(addTaskFAB);
            View sbView = snackbar.getView();
            sbView.setBackgroundColor(ContextCompat.getColor(this, R.color.colorSecondaryBg));
            snackbar.show();
            myAdapterC.notifyItemRemoved(position);
            alertDialog.cancel();
        });

        negativeButton.setText(R.string.Cancel);
        negativeButton.setOnClickListener(v -> {
            alertDialog.cancel();
            myAdapterC.notifyItemChanged(position);
        });
        alertDialog.show();
    }

    private void deleteCompletedTasks() {

        final AlertDialog.Builder alert = new AlertDialog.Builder(ListViewActivity.this);
        View mView = getLayoutInflater().inflate(R.layout.continue_or_cancle, null);

        final TextView messageTextView = (TextView) mView.findViewById(R.id.message_text_view);
        Button positiveButton = (Button) mView.findViewById(R.id.positive_button);
        Button negativeButton = (Button) mView.findViewById(R.id.negative_button);

        alert.setView(mView);
        String message = "All completed tasks will be deleted permanently.";
        messageTextView.setText(message);

        alertDialog = alert.create();
        alertDialog.setCanceledOnTouchOutside(true);

        positiveButton.setText(R.string.Delete);
        positiveButton.setOnClickListener(v -> {
            manager.deleteCompletedTasks();
            Snackbar snackbar = Snackbar.make(recyclerViewI, R.string.AllCTDeleted, Snackbar.LENGTH_LONG);
            snackbar.setAnchorView(addTaskFAB);
            View sbView = snackbar.getView();
            sbView.setBackgroundColor(ContextCompat.getColor(this, R.color.colorSecondaryBg));
            snackbar.show();
            myAdapterC.notifyDataSetChanged();
            alertDialog.cancel();
        });

        negativeButton.setText(R.string.Cancel);
        negativeButton.setOnClickListener(v -> alertDialog.cancel());
        alertDialog.show();
    }


    // Implemented methods of TaskCardViewListener!

    // Complete an incomplete task on checkbox click or swipe
    @Override
    public void completeTask(final int position) {
        final Task task = manager.getOpenList().getIncompleteTask(position);
        boolean deepComplete = task.getIncompleteSubTasksCount() == 0;
        int insertedAt = manager.setTaskComplete(position);
        String mssg = "Task Marked Complete";
        if (!deepComplete) {
           mssg = mssg + "\nSome sub-tasks were incomplete.";
        }
        myAdapterI.notifyItemRemoved(position);
        myAdapterC.notifyItemInserted(insertedAt);

        Snackbar snackbar = Snackbar.make(recyclerViewI, mssg, Snackbar.LENGTH_LONG);
        snackbar.setAnchorView(addTaskFAB);
        snackbar.setActionTextColor(ContextCompat.getColor(this, R.color.colorAccentSecondary));
        snackbar.setAction(R.string.Undo, v -> {
            int deletedFrom = manager.undoSetTaskComplete(task ,position);
            myAdapterI.notifyItemInserted(position);
            myAdapterC.notifyItemRemoved(deletedFrom);
        });
        View sbView = snackbar.getView();
        sbView.setBackgroundColor(ContextCompat.getColor(this, R.color.colorSecondaryBg));
        snackbar.show();
    }

    // Incomplete a completed task on checkbox click or swipe
    @Override
    public void incompleteTask(final int position) {
        final Task task = manager.getOpenList().getCompletedTask(position);
        int insertedAt = manager.setTaskIncomplete(position);
        myAdapterC.notifyItemRemoved(position);
        myAdapterI.notifyItemInserted(insertedAt);

        Snackbar snackbar = Snackbar.make(recyclerViewC, R.string.TaskMarkedAsIncomplete, Snackbar.LENGTH_LONG);
        snackbar.setAnchorView(addTaskFAB);
        snackbar.setActionTextColor(ContextCompat.getColor(this, R.color.colorAccentSecondary));
        snackbar.setAction(R.string.Undo, v -> {
            int deletedFrom = manager.undoSetTaskIncomplete(task ,position);
            myAdapterC.notifyItemInserted(position);
            myAdapterI.notifyItemRemoved(deletedFrom);
        });
        View sbView = snackbar.getView();
        sbView.setBackgroundColor(ContextCompat.getColor(this, R.color.colorSecondaryBg));
        snackbar.show();
    }

    // To add the "Completed" text view only when there are completed items
    @Override
    public void setCompletedTaskListTitle(String s) {
        completedTaskListTitle.setText(s);
    }

    @Override
    public void onTaskContainerClick(int position, boolean complete) {
        startTaskActivity(position, complete);
    }

    @Override
    public void onSubTaskItemClick(int taskPosition, int subTaskPosition, boolean complete) {
        // for incomplete sub task
        startTaskActivity(taskPosition, complete);
    }


    // function related to UI
    // show option bottom sheet
    private void showOptions() {
        final View dialogView = getLayoutInflater().inflate(R.layout.list_view_option_bottom_sheet, null);
        final BottomSheetDialog dialog = new BottomSheetDialog(this);
        dialog.setContentView(dialogView);

        // Different options
        // Delete Completed Tasks option
        LinearLayout deleteCompletedTaskOption = (LinearLayout) dialogView.findViewById(R.id.list_view_delete_complete_tasks_option);
        if (manager.getOpenList().completeTaskCount() > 0) {
            TextView textView = (TextView) dialogView.findViewById(R.id.list_view_delete_complete_tasks_option_tv);
            textView.setTextColor(ContextCompat.getColor(this, R.color.colorPrimaryText));
            deleteCompletedTaskOption.setOnClickListener(v -> {
                deleteCompletedTasks();
                dialog.cancel();
            });
        } else {
            TextView textView = (TextView) dialogView.findViewById(R.id.list_view_delete_complete_tasks_option_tv);
            textView.setTextColor(ContextCompat.getColor(this, R.color.colorUnavailableText));
        }

        // Delete the list option
        LinearLayout deleteListOption = (LinearLayout) dialogView.findViewById(R.id.list_view_delete_list_option);
        if (manager.getOpenListPosition() == 0) {
            TextView textView = (TextView) dialogView.findViewById(R.id.list_view_delete_list_option_tv);
            textView.setTextColor(ContextCompat.getColor(this, R.color.colorUnavailableText));
            textView.setText(R.string.DefaultListDelete);
        } else {
            TextView textView = (TextView) dialogView.findViewById(R.id.list_view_delete_list_option_tv);
            textView.setTextColor(ContextCompat.getColor(this, R.color.colorPrimaryText));
            deleteListOption.setOnClickListener(v -> {
                deleteList();
                dialog.cancel();
            });
        }

        // Rename list
        LinearLayout renameListOption = (LinearLayout) dialogView.findViewById(R.id.list_view_rename_list_option);
        renameListOption.setOnClickListener(v -> {
            renameList();
            dialog.cancel();
        });

        // Sort by option
        LinearLayout sortByOption = (LinearLayout) dialogView.findViewById(R.id.list_view_sort_option);
        TextView sortByOptionTv = (TextView) dialogView.findViewById(R.id.list_view_sort_option_tv);
        final int order = manager.getOpenListComparator();
        if (order == Manager.MY_ORDER) {
            sortByOptionTv.setText(R.string.MyOrder);
        } else if (order == Manager.CREATED_DATE) {
            sortByOptionTv.setText(R.string.CreationDate);
        } else if (order == Manager.DUE_DATE) {
            sortByOptionTv.setText(R.string.DueDate);
        }
        sortByOption.setOnClickListener(v -> {
            setSortOrder(order);
            dialog.cancel();
        });

        dialog.show();
    }


    // Methods related to the list :
    // show dialog to rename list
    private void renameList() {
        final AlertDialog.Builder alert = new AlertDialog.Builder(ListViewActivity.this);
        View mView = getLayoutInflater().inflate(R.layout.new_task_list_cd, null);

        TextView header = (TextView) mView.findViewById(R.id.new_task_list_cd_header);
        header.setText(R.string.RenameList);
        final EditText taskListTitleET = (EditText) mView.findViewById(R.id.new_task_list_title_ET);
        Button renameListButton = (Button) mView.findViewById(R.id.add_new_list_button);

        alert.setView(mView);
        alertDialog = alert.create();
        alertDialog.setCanceledOnTouchOutside(true);

        taskListTitleET.setText(manager.getListTitle());
        taskListTitleET.setOnFocusChangeListener((v, hasFocus) -> taskListTitleET.post(() -> {
            InputMethodManager inputMethodManager= (InputMethodManager) ListViewActivity.this.getSystemService(Context.INPUT_METHOD_SERVICE);
            inputMethodManager.showSoftInput(taskListTitleET, InputMethodManager.SHOW_IMPLICIT);
        }));

        renameListButton.setOnClickListener(v -> {
            String title = taskListTitleET.getText().toString().trim();
            if (title.equals("")) {
                Toast.makeText(ListViewActivity.this, "Provide a valid List title", Toast.LENGTH_SHORT).show();
            }
            else {
                manager.renameOpenList(title);
                makeToast("List renamed");
                setListView();
            }
            alertDialog.dismiss();
        });
        alertDialog.show();
        taskListTitleET.requestFocus();
    }

    // delete list?
    private void deleteList() {
        final AlertDialog.Builder alert = new AlertDialog.Builder(ListViewActivity.this);
        View mView = getLayoutInflater().inflate(R.layout.continue_or_cancle, null);

        final TextView messageTextView = (TextView) mView.findViewById(R.id.message_text_view);
        Button positiveButton = (Button) mView.findViewById(R.id.positive_button);
        Button negativeButton = (Button) mView.findViewById(R.id.negative_button);

        alert.setView(mView);
        String message = "This list will be deleted permanently.";
        messageTextView.setText(message);
        alertDialog = alert.create();
        alertDialog.setCanceledOnTouchOutside(true);

        positiveButton.setText(R.string.Delete);
        positiveButton.setOnClickListener(v -> {
            manager.deleteTaskList();
            alertDialog.cancel();
            makeToast("One list deleted");
            startShowLists();
        });

        negativeButton.setText(R.string.Cancel);
        negativeButton.setOnClickListener(v -> alertDialog.cancel());
        alertDialog.show();
    }

    // show bottom sheet to selec sort order
    private void setSortOrder(int order) {
        final View dialogView = getLayoutInflater().inflate(R.layout.list_view_sort_order_option, null);
        final BottomSheetDialog dialog = new BottomSheetDialog(this);
        dialog.setContentView(dialogView);

        LinearLayout myOrderOption = (LinearLayout) dialogView.findViewById(R.id.list_view_sort_my_order_option);
        TextView myOrderTV = (TextView) dialogView.findViewById(R.id.list_view_sort_my_order_option_tv);
        LinearLayout cDateOption = (LinearLayout) dialogView.findViewById(R.id.list_view_sort_creation_order_option);
        TextView cDateTV = (TextView) dialogView.findViewById(R.id.list_view_sort_creation_order_option_tv);
        LinearLayout dDateOption = (LinearLayout) dialogView.findViewById(R.id.list_view_sort_due_order_option);
        TextView dDateTV = (TextView) dialogView.findViewById(R.id.list_view_sort_due_order_option_tv);

        if (order == Manager.MY_ORDER) {
            myOrderTV.setTextColor(ContextCompat.getColor(this, R.color.colorAccentSecondary));
        } else if (order == Manager.CREATED_DATE) {
            cDateTV.setTextColor(ContextCompat.getColor(this, R.color.colorAccentSecondary));
        } else if (order == Manager.DUE_DATE) {
            dDateTV.setTextColor(ContextCompat.getColor(this, R.color.colorAccentSecondary));
        }

        myOrderOption.setOnClickListener(v -> {
            manager.setOpenListComparator(Manager.MY_ORDER);
            dialog.cancel();
            setListView();
        });

        cDateOption.setOnClickListener(v -> {
            manager.setOpenListComparator(Manager.CREATED_DATE);
            dialog.cancel();
            setListView();
        });

        dDateOption.setOnClickListener(v -> {
            manager.setOpenListComparator(Manager.DUE_DATE);
            dialog.cancel();
            setListView();
        });

        dialog.show();
    }


    // Methods related to staring a new activity
    public void startShowLists() {
        Intent myIntent = new Intent(this, ShowListsActivity.class);
        startActivity(myIntent);
    }

    private void startTaskActivity(int position, boolean complete) {
        Intent myIntent = new Intent(this, TaskViewActivity.class);
        myIntent.putExtra("task_position", position);
        myIntent.putExtra("task_status", complete);
        startActivity(myIntent);
    }


    @Override
    protected void onResume() {
        super.onResume();
        manager = new Manager(this);
        setListView();
    }

    /*****************************************************
     *  Recycler view ItemTouchHelper
     *****************************************************/
    // for incomplete task(Recycler View)
    ItemTouchHelper.SimpleCallback simpleCallback = new ItemTouchHelper.SimpleCallback((ItemTouchHelper.UP | ItemTouchHelper.DOWN), ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
        @Override
        public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
            if (manager.getOpenListComparator() != Manager.MY_ORDER)
                return false;

            // getting positions
            int fromPosition = viewHolder.getAdapterPosition();
            int toPosition = target.getAdapterPosition();

            // swapping in database
            manager.swapIncompleteTasks(fromPosition, toPosition);

            // notifying the adapter
            recyclerView.getAdapter().notifyItemMoved(fromPosition, toPosition);
            return true;
        }

        @Override
        public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
            int position = viewHolder.getAdapterPosition();
            switch (direction) {
                case ItemTouchHelper.LEFT:
                    deleteIncompleteTask(position);
                    break;
                case ItemTouchHelper.RIGHT:
                    completeTask(position);
                    break;
            }
        }

        @Override
        public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {
            new RecyclerViewSwipeDecorator.Builder(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
                    .addSwipeRightActionIcon(R.drawable.ic_baseline_done_24_accent)
                    .addSwipeLeftActionIcon(R.drawable.ic_baseline_delete_24_accent)
                    .addBackgroundColor(ContextCompat.getColor(ListViewActivity.this, R.color.colorAccentPrimary))
                    .create()
                    .decorate();
            super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
        }

        @Override
        public void onSelectedChanged(@Nullable RecyclerView.ViewHolder viewHolder, int actionState) {
            super.onSelectedChanged(viewHolder, actionState);
            if (actionState == ItemTouchHelper.ACTION_STATE_DRAG) {
                viewHolder.itemView.setBackgroundColor(ContextCompat.getColor(ListViewActivity.this, R.color.colorSecondaryBg));
                viewHolder.itemView.findViewById(R.id.task_card_view_sub_task_recycler_view).setVisibility(View.GONE);
            }
        }

        @Override
        public void clearView(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
            super.clearView(recyclerView, viewHolder);
            viewHolder.itemView.setBackgroundColor(ContextCompat.getColor(ListViewActivity.this, R.color.colorPrimaryBg));
            viewHolder.itemView.findViewById(R.id.task_card_view_sub_task_recycler_view).setVisibility(View.VISIBLE);
        }
    };

    // for complete task(RecyclerViewComplete)
    ItemTouchHelper.SimpleCallback simpleCallbackComplete = new ItemTouchHelper.SimpleCallback((ItemTouchHelper.UP | ItemTouchHelper.DOWN), ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
        @Override
        public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
            if (manager.getOpenListComparator() != Manager.MY_ORDER)
                return false;

            // getting positions
            int fromPosition = viewHolder.getAdapterPosition();
            int toPosition = target.getAdapterPosition();

            // swapping in database
            manager.swapCompleteTasks(fromPosition, toPosition);

            // notifying the adapter
            recyclerView.getAdapter().notifyItemMoved(fromPosition, toPosition);
            return true;
        }

        @Override
        public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
            int position = viewHolder.getAdapterPosition();
            switch (direction) {
                case ItemTouchHelper.LEFT:
                    deleteCompleteTask(position);
                    break;
                case ItemTouchHelper.RIGHT:
                    incompleteTask(position);
            }
        }

        @Override
        public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {
            new RecyclerViewSwipeDecorator.Builder(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
                    .addSwipeRightActionIcon(R.drawable.ic_baseline_radio_button_unchecked_24)
                    .addSwipeLeftActionIcon(R.drawable.ic_baseline_delete_24_accent)
                    .addBackgroundColor(ContextCompat.getColor(ListViewActivity.this, R.color.colorAccentPrimary))
                    .create()
                    .decorate();
            super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
        }

        @Override
        public void onSelectedChanged(@Nullable RecyclerView.ViewHolder viewHolder, int actionState) {
            super.onSelectedChanged(viewHolder, actionState);
            if (actionState == ItemTouchHelper.ACTION_STATE_DRAG) {
                viewHolder.itemView.setBackgroundColor(ContextCompat.getColor(ListViewActivity.this, R.color.colorSecondaryBg));
            }
        }

        @Override
        public void clearView(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
            super.clearView(recyclerView, viewHolder);
            viewHolder.itemView.setBackgroundColor(ContextCompat.getColor(ListViewActivity.this, R.color.colorPrimaryBg));
        }
    };

    public void makeToast(String str) {
        Toast.makeText(this, str, Toast.LENGTH_SHORT).show();
    }
}
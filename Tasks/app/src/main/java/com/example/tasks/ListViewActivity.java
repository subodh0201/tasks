package com.example.tasks;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.graphics.Canvas;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.tasks.adapters.RecyclerAdapterCT;
import com.example.tasks.adapters.RecyclerAdapterIT;
import com.example.tasks.interfaces.TaskCardViewListener;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import it.xabaras.android.recyclerview.swipedecorator.RecyclerViewSwipeDecorator;

public class ListViewActivity extends AppCompatActivity implements TaskCardViewListener {

    private Manager manager;                // manager object
    private TextView listTitleTextView;     // title of the list!

    public TextView completedTaskListTitle; // weird stuff

    private RecyclerView recyclerViewI;     // for incomplete tasks
    private RecyclerAdapterIT myAdapterI;           // adapter for incomplete task

    private RecyclerView recyclerViewC;     // for complete tasks
    private RecyclerAdapterCT myAdapterC;
    private AlertDialog alertDialog;


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
        showAllLists.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startShowLists();
            }
        });

        //  show options image button
        ImageButton optionsButton = (ImageButton) findViewById(R.id.list_view_options);
        optionsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showOptions();
            }
        });

        //  show setting image button
        ImageButton settingsButton = (ImageButton) findViewById(R.id.list_view_settings_button);
        settingsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showSettings();
            }
        });


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
        FloatingActionButton addTaskFAB = (FloatingActionButton) findViewById(R.id.add_task_fab);
        addTaskFAB.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showAddTaskDialog();
            }
        });


        // set up the list view for the first time
        setListView();
    }

    // update the View
    private void setListView() {
        // Set title of list in view
        listTitleTextView.setText(manager.getListTitle());

        // for incomplete tasks
        myAdapterI = new RecyclerAdapterIT(manager.getOpenList(), this);
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
        Button addNewTasButton = (Button) mView.findViewById(R.id.add_new_task_button);

        alert.setView(mView);

        alertDialog = alert.create();
        alertDialog.setCanceledOnTouchOutside(true);

        addNewTasButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String name = taskTitleET.getText().toString();
                String detail = taskDetailET.getText().toString();
                detail.trim();
                name = name.trim();
                if (name.equals("")) {
                    makeToast("Provide a valid Task title.");
                    alertDialog.dismiss();
                } else {
                    manager.addTask(name, detail, null);
                    alertDialog.dismiss();
                    Toast.makeText(ListViewActivity.this, "New Task added!", Toast.LENGTH_SHORT).show();

                    myAdapterI.notifyDataSetChanged();
                }
            }
        });
        alertDialog.show();
    }

    // delete an incomplete task on swipe
    private void deleteIncompleteTask(final int position) {

        final AlertDialog.Builder alert = new AlertDialog.Builder(ListViewActivity.this);
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
        positiveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                manager.deleteIncompleteTask(position);
                myAdapterI.notifyItemRemoved(position);
                myAdapterI.notifyItemRangeChanged(position, myAdapterI.getItemCount());
                alertDialog.cancel();
            }
        });

        negativeButton.setText(R.string.Cancel);
        negativeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                alertDialog.cancel();
                myAdapterI.notifyDataSetChanged();
            }
        });
        alertDialog.show();
    }

    // delete a complete task on swipe
    private void deleteCompleteTask(final int position) {
        final AlertDialog.Builder alert = new AlertDialog.Builder(ListViewActivity.this);
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
        positiveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                manager.deleteCompleteTask(position);
                myAdapterC.notifyItemRemoved(position);
                myAdapterC.notifyItemRangeChanged(position, myAdapterC.getItemCount());
                alertDialog.cancel();
            }
        });

        negativeButton.setText(R.string.Cancel);
        negativeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                alertDialog.cancel();
                myAdapterC.notifyDataSetChanged();
            }
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
        positiveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                manager.deleteCompletedTasks();
                myAdapterC.notifyDataSetChanged();
                alertDialog.cancel();
            }
        });

        negativeButton.setText(R.string.Cancel);
        negativeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                alertDialog.cancel();
            }
        });
        alertDialog.show();
    }




    // Implemented methods of TaskCardViewListener!

    // Complete an incomplete task on checkbox click or swipe
    @Override
    public void completeTask(int position) {
        boolean deepComplete = manager.setTaskComplete(position);
        if (!deepComplete) {
            makeToast("Some sub-tasks were incomplete!");
        }
        myAdapterI.notifyItemRemoved(position);
        myAdapterI.notifyItemRangeChanged(position, myAdapterI.getItemCount());

        myAdapterC.notifyDataSetChanged();
    }

    // Incomplete a completed task on checkbox click or swipe
    @Override
    public void incompleteTask(int position) {
        manager.setTaskIncomplete(position);

        myAdapterC.notifyItemRemoved(position);
        myAdapterC.notifyItemRangeChanged(position, myAdapterC.getItemCount());

        myAdapterI.notifyDataSetChanged();
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
            deleteCompletedTaskOption.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    deleteCompletedTasks();
                    dialog.cancel();
                }
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
            deleteListOption.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    deleteList();
                    dialog.cancel();
                }
            });
        }

        // Rename list
        LinearLayout renameListOption = (LinearLayout) dialogView.findViewById(R.id.list_view_rename_list_option);
        renameListOption.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                renameList();
                dialog.cancel();
            }
        });

        // Sort by option
        LinearLayout sortByOption = (LinearLayout) dialogView.findViewById(R.id.list_view_sort_option);
        TextView sortByOptionTv = (TextView) dialogView.findViewById(R.id.list_view_sort_option_tv);
        final int order = manager.getOpenListComparator();
        if (order == Manager.MY_ORDER) {
            sortByOptionTv.setText(R.string.MyOrder);
        } else if (order == Manager.CREATED_DATE) {
            sortByOptionTv.setText(R.string.CreationDate);
        }
        sortByOption.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setSortOrder(order);
                dialog.cancel();
            }
        });

        dialog.show();
    }

    // show setting botton sheet
    private void showSettings() {
        final View dialogView = getLayoutInflater().inflate(R.layout.settings_bottom_sheet, null);
        final BottomSheetDialog dialog = new BottomSheetDialog(this);
        dialog.setContentView(dialogView);

        LinearLayout themeOption = (LinearLayout) dialogView.findViewById(R.id.settings_theme_option);
        TextView themeTV = (TextView) dialogView.findViewById(R.id.settings_theme_option_tv);
        if (manager.getUser().isDarkModeOn()) {
            themeTV.setText(R.string.Dark);
        } else {
            themeTV.setText(R.string.Light);
        }
        themeOption.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (manager.getUser().isDarkModeOn()) {
                    manager.getUser().setDarkModeOff();
                } else {
                    manager.getUser().setDarkModeOn();
                }
                if (manager.getUser().isDarkModeOn()) {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                } else {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                }
                manager.write();
                dialog.cancel();
            }
        });
        dialog.show();
    }


    // Methods related to the list :
    // show dialog to rename list
    private void renameList() {
        final AlertDialog.Builder alert = new AlertDialog.Builder(ListViewActivity.this);
        View mView = getLayoutInflater().inflate(R.layout.new_task_list_cd, null);

        final EditText taskListTitleET = (EditText) mView.findViewById(R.id.new_task_list_title_ET);
        Button renameListButton = (Button) mView.findViewById(R.id.add_new_list_button);

        alert.setView(mView);

        alertDialog = alert.create();
        alertDialog.setCanceledOnTouchOutside(true);

        taskListTitleET.setText(manager.getListTitle());

        renameListButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String title = taskListTitleET.getText().toString().trim();
                if (title.equals("")) {
                    Toast.makeText(ListViewActivity.this, "Provide a valid List title.", Toast.LENGTH_SHORT).show();
                }
                else {
                    manager.renameOpenList(title);
                    setListView();
                }
                alertDialog.dismiss();
            }
        });
        alertDialog.show();
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
        positiveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                manager.deleteTaskList();
                startShowLists();
            }
        });

        negativeButton.setText(R.string.Cancel);
        negativeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                alertDialog.cancel();
            }
        });
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

        if (order == Manager.MY_ORDER) {
            myOrderTV.setTextColor(ContextCompat.getColor(this, R.color.colorAccentSecondary));
        } else if (order == Manager.CREATED_DATE) {
            cDateTV.setTextColor(ContextCompat.getColor(this, R.color.colorAccentSecondary));
        }

        myOrderOption.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                manager.setOpenListComparator(Manager.MY_ORDER);
                dialog.cancel();
                startListView();
            }
        });

        cDateOption.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                manager.setOpenListComparator(Manager.CREATED_DATE);
                dialog.cancel();
                startListView();
            }
        });
        dialog.show();

    }




    // Methods related to staring a new activity
    public void startShowLists() {
        Intent myIntent = new Intent(this, ShowListsActivity.class);
        startActivity(myIntent);
        finish();
    }

    private void startListView() {
        Intent myIntent = new Intent(this, ListViewActivity.class);
        startActivity(myIntent);
        finish();
    }

    private void startTaskActivity(int position, boolean complete) {
        Intent myIntent = new Intent(this, TaskViewActivity.class);
        myIntent.putExtra("task_position", position);
        myIntent.putExtra("task_status", complete);
        startActivity(myIntent);
        finish();
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
            }
        }

        @Override
        public void clearView(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
            super.clearView(recyclerView, viewHolder);
            viewHolder.itemView.setBackgroundColor(ContextCompat.getColor(ListViewActivity.this, R.color.colorPrimaryBg));
        }
    };

    // for complete task(RecyclerViewComplete)
    ItemTouchHelper.SimpleCallback simpleCallbackComplete = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
        @Override
        public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
            return false;
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
                    .addSwipeRightActionIcon(R.drawable.ic_baseline_check_box_outline_blank_24_accent)
                    .addSwipeLeftActionIcon(R.drawable.ic_baseline_delete_24_accent)
                    .addBackgroundColor(ContextCompat.getColor(ListViewActivity.this, R.color.colorAccentPrimary))
                    .create()
                    .decorate();
            super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
        }
    };


    public void makeToast(String str) {
        Toast.makeText(this, str, Toast.LENGTH_SHORT).show();
    }
}
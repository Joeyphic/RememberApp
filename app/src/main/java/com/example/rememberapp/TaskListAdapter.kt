package com.example.rememberapp

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.ItemTouchHelper.ACTION_STATE_DRAG
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.rememberapp.data.Task
import com.example.rememberapp.data.getColorByPriority
import com.example.rememberapp.databinding.TaskListItemFragmentBinding
import java.util.*

class TaskListAdapter(private val onTaskClicked: (Task) -> Unit) :
    ListAdapter<Task, TaskListAdapter.TaskViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, ViewType: Int): TaskViewHolder {
        return TaskViewHolder(
            TaskListItemFragmentBinding.inflate(
                LayoutInflater.from(
                    parent.context
                )
            )
        )
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        val currentTask = getItem(position)
        holder.itemView.setOnClickListener {
            onTaskClicked(currentTask)
        }
        holder.bind(currentTask)
    }

    // TODO: Determine if this is a necessary function
    override fun getItemViewType(position: Int) : Int {
        val currentTask = getItem(position)
        return currentTask.taskPriority.ordinal
    }

    class TaskViewHolder(private var binding: TaskListItemFragmentBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(task: Task) {
            binding.apply {
                taskName.text = task.taskName
                relativeLayout.setBackgroundColor(task.getColorByPriority())
            }
        }
    }

    companion object {
        private val DiffCallback = object : DiffUtil.ItemCallback<Task>() {
            override fun areItemsTheSame(oldItem: Task, newItem: Task): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: Task, newItem: Task): Boolean {
                return oldItem == newItem
            }
        }
    }

    sealed class DataItem {

        abstract val id: Int

        data class TaskListItem(val task: Task): DataItem() {
            override val id = task.id
        }

        object Header: DataItem() {
            override val id = Int.MIN_VALUE
        }
    }

    // TODO: Finish implementing ItemTouchHelper
    class TaskTouchHelper(
        private val adapter: TaskListAdapter,
        private val onItemMove: (from: Int, to: Int) -> Unit
    ) : ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0) {

        lateinit var temporaryList : MutableList<Task>
        private var from = -1
        private var to = -1

        override fun onSelectedChanged(viewHolder: RecyclerView.ViewHolder?, actionState: Int) {
            super.onSelectedChanged(viewHolder, actionState)

            if(actionState == ACTION_STATE_DRAG && viewHolder != null) {
                from = viewHolder.adapterPosition

                viewHolder.itemView.background.alpha = 200

                // TODO: Discover why we need to change elevation to stop dragged view going
                //  underneath other views.

                viewHolder.itemView.elevation = 0.01F
                temporaryList = adapter.currentList.toMutableList()
                adapter.submitList(temporaryList)
            }
        }

        override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
            to = viewHolder.adapterPosition
            viewHolder.itemView.background.alpha = 255
            viewHolder.itemView.elevation = 0.0F
            /*
            for(i in from..to) {
                temporaryList[i].taskSortOrder = i
            }

            for(i in from downTo to) {
                temporaryList[i].taskSortOrder = i
            }
            */

            for(i in temporaryList.indices) {
                temporaryList[i].taskSortOrder = i
            }

            onItemMove(from, to)
        }

        // Idea is use onMove to check Priority between Tasks and move if same.
        // Can set position at beginning (selChanged) and end (clrView), then pass that value
        // to database with orderPosition value in ViewModel.
        // notifyItemMoved seems to be the key BC theres no previous rev. (In dev diary)

        override fun onMove(
            recyclerView: RecyclerView,
            viewHolder: RecyclerView.ViewHolder,
            target: RecyclerView.ViewHolder
        ): Boolean {
            Log.i("TaskListAdapter", "vh pos " + viewHolder.adapterPosition)
            Log.i("TaskListAdapter", "target pos " + target.adapterPosition)
            Log.i("TaskListAdapter", "list: " + adapter.currentList)
            // Although adapter is passed parameter, we can still use adapterPosition since we
            // are in TaskListAdapter.kt. That's why this class is in the adapter!

            val taskPositionFrom = viewHolder.adapterPosition
            val taskPositionTo = target.adapterPosition

            if(temporaryList[taskPositionFrom].taskPriority == temporaryList[taskPositionTo].taskPriority)
            {
                val currentTask = temporaryList[taskPositionFrom]
                temporaryList.removeAt(taskPositionFrom)
                temporaryList.add(taskPositionTo, currentTask)

                adapter.notifyItemMoved(taskPositionFrom, taskPositionTo)
            }
            else return false // solves unintentional behaviour of dragged item showing under target

            Log.i("TaskListAdapter", "list2: " + adapter.currentList)
            return true
        }

        override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
            return // Swiping is not supported in this list
        }
    }
}
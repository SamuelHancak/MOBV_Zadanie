package eu.mcomputing.mobv.mobvzadanie.utils

import android.view.View
import androidx.databinding.BindingAdapter
import androidx.recyclerview.widget.DiffUtil
import com.google.android.material.snackbar.Snackbar
import eu.mcomputing.mobv.mobvzadanie.data.db.entities.UserEntity

class ItemDiffCallback(
    private val oldList: List<UserEntity>,
    private val newList: List<UserEntity>
) : DiffUtil.Callback() {
    override fun getOldListSize() = oldList.size
    override fun getNewListSize() = newList.size
    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldList[oldItemPosition].uid == newList[newItemPosition].uid
    }

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldList[oldItemPosition] == newList[newItemPosition]
    }
}

@BindingAdapter(
    "showTextToast"
)
fun applyShowTextToast(
    view: View,
    message: Evento<String>?
) {
    message?.getContentIfNotHandled()?.let {
        if (it.isNotBlank()) {
            Snackbar.make(view, it, Snackbar.LENGTH_SHORT).show()
        }
    }
}
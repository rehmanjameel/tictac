package io.xconn.tictackotlin.adapter

import android.content.Context
import android.content.Intent
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import de.hdodenhof.circleimageview.CircleImageView
import io.xconn.tictackotlin.App
import io.xconn.tictackotlin.MainActivity
import io.xconn.tictackotlin.R
import io.xconn.tictackotlin.model.OnlineUsersModel
import kotlinx.coroutines.runBlocking

class OnlineUsersAdapter(val context: Context, private val usersList: ArrayList<OnlineUsersModel>) :
    RecyclerView.Adapter<OnlineUsersAdapter.ViewHolder>() {

    private val app = App()

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textView: TextView = itemView.findViewById(R.id.textViewUserName)
        val userImage: CircleImageView = itemView.findViewById(R.id.imageViewAvatar)
        val userCard: RelativeLayout = itemView.findViewById(R.id.userCard)

    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.online_users_layout, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val user = usersList[position]
        holder.textView.text = user.name

        holder.userCard.setOnClickListener {
            runBlocking {
                sendRequest(user)
            }
        }

    }

    override fun getItemCount(): Int {
        return usersList.size
    }

    private suspend fun sendRequest(user: OnlineUsersModel) {
        val result = App.session.call("io.xconn.tictac.${user.id}.pair",
            args = listOf(app.getValueInt("user_id")))
        context.startActivity(Intent(context, MainActivity::class.java).apply {
            putExtra("user_id", user.id)
        })
        Log.e("pair result", result.toString())
    }

    // Update list dynamically
    fun updateList(newUsers: ArrayList<OnlineUsersModel>) {
        usersList.clear()
        usersList.addAll(newUsers)
        notifyDataSetChanged()
    }
}
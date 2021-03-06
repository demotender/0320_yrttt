package com.ryan.chat

import android.app.Activity
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.ryan.chat.databinding.FragmentRoomBinding
import com.ryan.chat.databinding.RowMessageBinding
import okhttp3.*
import okio.ByteString
import java.util.concurrent.TimeUnit

class RoomFragment : Fragment() {
        companion object {
            val TAG = RoomFragment::class.java.simpleName
            val instance : RoomFragment by lazy {
                RoomFragment()
            }
        }
        lateinit var binding: FragmentRoomBinding
        lateinit var websocket: WebSocket
        lateinit var adapter : RoomMessageAdapter
        val messageViewModel by viewModels<MessageViewModel>()

        // 測試用 直播室網址（圖片版
        val mapOfRoomId = mapOf(
            "5015" to "https://i.imgur.com/bou4Cag.jpg", // 水水
            "5013" to "https://i.imgur.com/WbSlIAX.jpg", // 可比
            "5019" to "https://i.imgur.com/SdsqyXM.jpg", // 乐乐
            "5018" to "https://i.imgur.com/fPeogox.jpg", // 跳跳
            "5011" to "https://i.imgur.com/DUFDOxV.jpg", // Bee
            "5007" to "https://i.imgur.com/P5HmYNP.jpg", // 凌晨🌛
            "5016" to "https://i.imgur.com/dBnoHFo.jpg", // 妍淨
            "5014" to "https://i.imgur.com/NMG1Bf3.jpg", // 佳佳
            "5010" to "https://i.imgur.com/sb2J0TF.jpg", // 燕子
            "5012" to "https://i.imgur.com/VqtHiV6.jpg", // 肉肉
            "4971" to "https://i.imgur.com/viHyLC0.jpg", // 直播小帮手
            "5020" to "https://i.imgur.com/0QucvHy.jpg", // 小檸檬
            "5003" to "https://i.imgur.com/eI8KK9I.jpg", // 暖暖
            "5008" to "https://i.imgur.com/D1r3OYl.jpg", // 安苡萱
            "4972" to "https://i.imgur.com/BLUSgdg.jpg", // 直播小帮手
            "5017" to "https://i.imgur.com/jRv6i92.jpg", // 錢錢
        )

        override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
        ): View? {
            binding = FragmentRoomBinding.inflate(inflater)
//        return super.onCreateView(inflater, container, savedInstanceState)
            return binding.root
        }

        override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
            super.onViewCreated(view, savedInstanceState)
            // 由此處開始寫 code
            val parentActivity =  requireActivity() as MainActivity
            val prefLogin = requireContext().getSharedPreferences("login", AppCompatActivity.MODE_PRIVATE)
            val prefUser = requireContext().getSharedPreferences("userinfo", AppCompatActivity.MODE_PRIVATE)
            var login = prefLogin.getBoolean("login_state", false)
            var user = prefLogin.getString("login_userid", "")
            var username = prefUser.getString("${user}name", "guest")
            var requestName = "guest"

            if (login) {
                requestName = username.toString()
            }

            var path = "girl"
            var vidPath = "android.resource://"+requireContext().packageName+"/raw/$path"
            var uri = Uri.parse(vidPath)

            val client = OkHttpClient.Builder()
                .readTimeout(3, TimeUnit.SECONDS)
                .build()
            val request = Request.Builder()
                .url("wss://lott-dev.lottcube.asia/ws/chat/chat:app_test?nickname=$requestName")
                .build()

            websocket = client.newWebSocket(request, object : WebSocketListener() {
                override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                    super.onClosed(webSocket, code, reason)
                    Log.d(TAG, ": onClosed");
                }

                override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                    super.onClosing(webSocket, code, reason)
                    Log.d(TAG, ": onClosing");
                }

                override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                    super.onFailure(webSocket, t, response)
                    Log.d(TAG, ": onFailure")
                }

                override fun onMessage(webSocket: WebSocket, text: String) {
                    super.onMessage(webSocket, text)
                    val json = text
                    var singleMessage = ""
                    if ("sys_updateRoomStatus" in json) {
                        val response = Gson().fromJson(json, UpdateRoomStatus::class.java)
                        var action = response.body.entry_notice.action
                        singleMessage =
                            when (action)  {
                                "enter" -> "歡迎 ${response.body.entry_notice.username} 進到直播間"
//                                          Log.d(TAG, "歡迎 ${response.body.entry_notice.username} 進到直播間")
                                "leave" ->  "${response.body.entry_notice.username} 已離開直播間"
    //                                      Log.d(TAG, " ${response.body.entry_notice.username} 已離開直播間")
                                else -> ""
                            }
                    } else if ("admin_all_broadcast" in json) {
                        val response = Gson().fromJson(json, AllBroadcast::class.java)
                        singleMessage = """
                            英文公告:${response.body.content.en}
                            繁體公告:${response.body.content.tw}
                            簡體公告:${response.body.content.cn}
                        """.trimIndent()
//                        Log.d(TAG, "英文公告:${response.body.content.en}")
//                        Log.d(TAG, "繁體公告:${response.body.content.tw}")
//                        Log.d(TAG, "簡體公告:${response.body.content.cn}")
                    } else if ("sys_room_endStream" in json) {
                        val response = Gson().fromJson(json, RoomEndStream::class.java)
                        singleMessage ="系統公告:${response.body.text}"
//                        Log.d(TAG, "系統公告:${response.body.text}")
                    } else if ("default_message" in json) {
                        val response = Gson().fromJson(json, ReceiveMessage::class.java)
                        singleMessage = "${response.body.nickname} : ${response.body.text}"
                    }
                    else if ("sys_member_notice" in json) {
                        val response = Gson().fromJson(json, MemberNotice::class.java)
                        val noticeMessage = response.body.text
                        activity?.runOnUiThread {
                            Toast.makeText(requireContext(), noticeMessage, Toast.LENGTH_LONG).show()
                        }
                        Log.d(TAG, noticeMessage)
                    }
                    else {
                        Log.d(TAG, "onMessage: $text")
                    }
                    messageViewModel.getMessages(singleMessage)
                }

                override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                    super.onMessage(webSocket, bytes)
                    Log.d(TAG, ": onMessage ${bytes.hex()}");
                }

                override fun onOpen(webSocket: WebSocket, response: Response) {
                    super.onOpen(webSocket, response)
                    Log.d(TAG, ": onOpen: response = $response")
                }
            })

            // 自動播放 Demo影片
            binding.vGirl.setVideoURI(uri)
            binding.vGirl.setOnPreparedListener {
                binding.vGirl.start()
            }

            /// 聊天視窗
            binding.msgRecycler.setHasFixedSize(true)
            binding.msgRecycler.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, true)
            adapter = RoomMessageAdapter()
            binding.msgRecycler.adapter = adapter

            messageViewModel.messages.observe(viewLifecycleOwner) { messages ->
                adapter.submitMessages(messages)
            }

            binding.btSend.setOnClickListener {
                var message = binding.edSendMessage.text.toString()
                var json = Gson().toJson(SendMessage("N", message))
                binding.edSendMessage.setText("")
                websocket.send(json)
            }

            binding.btLeave.setOnClickListener {
                AlertDialog.Builder(requireContext())
                    .setTitle("message")
                    .setMessage("Are you sure you want to leave?")
                    .setPositiveButton("Yes") { d, w ->

                        parentActivity.supportFragmentManager.beginTransaction().run {
                            replace(R.id.main_container, parentActivity.mainFragments[1])
                            replace(R.id.chat_container, parentActivity.chatFragments[0])
                            commit()
                        }
                        parentActivity.binding.bottonNavBar.visibility = View.VISIBLE
                    }
                    .setNegativeButton("No", null)
                    .show()

            }

        }
    inner class RoomMessageAdapter : RecyclerView.Adapter<MessageViewHolder>() {
        val sendMessage = mutableListOf<String>()
        override fun getItemCount(): Int {
            return sendMessage.size
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
            val binding = RowMessageBinding.inflate(layoutInflater, parent, false)
            return MessageViewHolder(binding)
        }

        override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
            val singleMessage = sendMessage[position]
            holder.messageContent.setText(singleMessage)
        }
        fun submitMessages(messages: String) {
            sendMessage.add(0, messages)
            notifyDataSetChanged()
        }

    }

    inner class MessageViewHolder(val binding: RowMessageBinding) :
            RecyclerView.ViewHolder(binding.root) {
                val messageContent = binding.tvRoomMessage
            }

}
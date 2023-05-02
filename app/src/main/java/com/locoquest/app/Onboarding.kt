package com.locoquest.app;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.fragment.app.Fragment;

class Onboarding : Fragment() {

    private lateinit var iv_map: ImageView;
    private lateinit var tv_map_description: TextView;
    private lateinit var iv_benchmark_data: ImageView;
    private lateinit var tv_benchmark_data_description: TextView;
    private lateinit var iv_coin_collection: ImageView;
    private lateinit var tv_coin_collection_description: TextView;
    private lateinit var iv_profile: ImageView;
    private lateinit var tv_profile_description: TextView;
    private lateinit var btn_get_started: Button;

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_onboarding, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize views
        iv_map = view.findViewById(R.id.iv_map)
        tv_map_description = view.findViewById(R.id.tv_map_description)
        iv_benchmark_data = view.findViewById(R.id.iv_benchmark_data)
        tv_benchmark_data_description = view.findViewById(R.id.tv_benchmark_data_description)
        iv_coin_collection = view.findViewById(R.id.iv_coin_collection)
        tv_coin_collection_description = view.findViewById(R.id.tv_coin_collection_description)
        iv_profile = view.findViewById(R.id.iv_profile)
        tv_profile_description = view.findViewById(R.id.tv_profile_description)
        btn_get_started = view.findViewById(R.id.btn_get_started)

        // Set up the map image and description
        iv_map.setImageResource(R.drawable.map_image)
        tv_map_description.text = getString(R.string.map_description)

        // Set up the benchmark data image and description
        iv_benchmark_data.setImageResource(R.drawable.benchmark_data_image)
        tv_benchmark_data_description.text = getString(R.string.benchmark_data_description)

        // Set up the coin collection image and description
        iv_coin_collection.setImageResource(R.drawable.coin_collection_image)
        tv_coin_collection_description.text = getString(R.string.coin_collection_description)

        // Set up the profile image and description
        iv_profile.setImageResource(R.drawable.profile_image)
        tv_profile_description.text = getString(R.string.profile_description)

        // Set up the "Get Started" button
        btn_get_started.setOnClickListener {
            // Handle the button click event here
        }
    }
}

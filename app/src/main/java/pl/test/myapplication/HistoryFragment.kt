package pl.test.myapplication

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.coroutines.launch
import pl.test.myapplication.databinding.FragmentHistoryBinding
import pl.test.myapplication.data.ResultRepository


class HistoryFragment : Fragment() {
    private var _binding: FragmentHistoryBinding? = null
    private val binding get() = _binding!!
    private val repo by lazy {ResultRepository(requireContext())}
    private val adapter = HistoryAdapter()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        _binding = FragmentHistoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.rvHistory.layoutManager = LinearLayoutManager(requireContext())
        binding.rvHistory.adapter = adapter

        binding.btnClearHistory.setOnClickListener {
            viewLifecycleOwner.lifecycleScope.launch {
                repo.clearAll()
                Toast.makeText(requireContext(), "Wyczyszczono historię", Toast.LENGTH_SHORT).show()
                refresh()
            }
        }
        refresh()
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    private fun refresh(){
        viewLifecycleOwner.lifecycleScope.launch {
            val items = repo.getAll()
            adapter.submitList(items)
            binding.txtEmpty.visibility = if (items.isEmpty()) View.VISIBLE else View.GONE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
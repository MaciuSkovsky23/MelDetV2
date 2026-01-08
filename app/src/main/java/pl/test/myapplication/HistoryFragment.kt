package pl.test.myapplication

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.coroutines.launch
import pl.test.myapplication.databinding.FragmentHistoryBinding
import pl.test.myapplication.data.ResultRepository


/*
    Ekran historii badan
    Pokazuje liste zapisanych wynikow, umozliwia czyszczenie historii i usuwanie pojedynczych wpisow
 */
class HistoryFragment : Fragment() {
    private var _binding: FragmentHistoryBinding? = null
    private val binding get() = _binding!!
    private val repo by lazy {ResultRepository(requireContext())}
    private val adapter by lazy{
        HistoryAdapter{entity ->
            val args = Bundle().apply { putLong("resultId", entity.id) }
            findNavController().navigate(R.id.action_historyFragment_to_resultDetailFragment, args)
        }}

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

        binding.btnBackToAnalyze.setOnClickListener {
            findNavController().popBackStack(R.id.analyzeFragment, false)
        }
        binding.btnClearHistory.setOnClickListener {
            viewLifecycleOwner.lifecycleScope.launch {
                repo.clearAll()
                Toast.makeText(requireContext(), "Wyczyszczono historię", Toast.LENGTH_SHORT).show()
                refresh()
            }
        }
        refresh()

        val swipe = object : androidx.recyclerview.widget.ItemTouchHelper.SimpleCallback(
            0,
            androidx.recyclerview.widget.ItemTouchHelper.LEFT or androidx.recyclerview.widget.ItemTouchHelper.RIGHT
        ) {
            override fun onMove(
                recyclerView: androidx.recyclerview.widget.RecyclerView,
                viewHolder: androidx.recyclerview.widget.RecyclerView.ViewHolder,
                target: androidx.recyclerview.widget.RecyclerView.ViewHolder
            ): Boolean = false

            override fun onSwiped(viewHolder: androidx.recyclerview.widget.RecyclerView.ViewHolder, direction: Int) {
//                val pos = viewHolder.bindingAdapterPosition
                val pos = viewHolder.adapterPosition
                if (pos == androidx.recyclerview.widget.RecyclerView.NO_POSITION) return
                val item = adapter.currentList.getOrNull(pos)

                if (item == null) {
                    adapter.notifyItemChanged(pos)
                    return
                }

                viewLifecycleOwner.lifecycleScope.launch {
                    repo.deleteOne(item)
                    Toast.makeText(requireContext(), "Usunięto wpis", Toast.LENGTH_SHORT).show()
                    refresh()
                }
            }
        }

        androidx.recyclerview.widget.ItemTouchHelper(swipe).attachToRecyclerView(binding.rvHistory)

    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    //odswieza liste historii z bazy room
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
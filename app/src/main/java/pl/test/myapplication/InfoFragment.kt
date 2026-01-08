package pl.test.myapplication

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import pl.test.myapplication.databinding.FragmentInfoBinding
import pl.test.myapplication.databinding.FragmentStartBinding

/*
    Ekran informacyjny
    Krotka informacja o celu oraz informacja o zalecanym kontakcie ze specjalista
    Klikniecie w dowolne miejsce przechodzi dalej
    Po 5 sekundach automatyczne przejscie dalej
 */
class InfoFragment : Fragment() {
    private var _binding: FragmentInfoBinding? = null
    private val binding get() = _binding!!

    private var autoNavJob: Job? = null
    private var navigated = false
    private var autoNext = true
    private val autoDelay = 5_000L

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentInfoBinding.inflate(layoutInflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.root.setOnClickListener {
//            findNavController().navigate(R.id.action_infoFragment_to_analyzeFragment)
            goNext()
        }
        if(autoNext){
        autoNavJob = viewLifecycleOwner.lifecycleScope.launch {
            delay(autoDelay)
            goNext()
            autoNext = false
            }
        }
    }

    private fun goNext(){
        if(navigated) return

        val stillHere = findNavController().currentDestination?.id == R.id.infoFragment
        if (!stillHere) return

        navigated = true
        autoNavJob?.cancel()

        findNavController().navigate(R.id.action_infoFragment_to_analyzeFragment)
    }

    override fun onResume() {
        super.onResume()
        navigated = false
    }

    override fun onPause() {
        super.onPause()
        autoNavJob?.cancel()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        autoNavJob?.cancel()
        _binding = null
    }
}
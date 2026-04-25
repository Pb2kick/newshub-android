package com.example.newshub.feature.news

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import coil.load
import com.example.newshub.R
import com.example.newshub.databinding.FragmentNewsDetailBinding

class NewsDetailFragment : Fragment() {

    private var _binding: FragmentNewsDetailBinding? = null
    private val binding get() = _binding!!

    private val viewModel: NewsDetailViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNewsDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val articleUrl = arguments?.getString("articleUrl")
        val articleTitle = arguments?.getString("articleTitle").orEmpty()
        val articleSource = arguments?.getString("articleSource").orEmpty()
        val articlePublishedAt = arguments?.getString("articlePublishedAt").orEmpty()
        val articleSummary = arguments?.getString("articleSummary").orEmpty()
        val articleCategory = arguments?.getString("articleCategory").orEmpty().ifBlank { "Top Stories" }
        val articleAuthor = arguments?.getString("articleAuthor").orEmpty().ifBlank { "NewsHub" }
        val articleReadTime = arguments?.getString("articleReadTime").orEmpty().ifBlank { "3 min read" }
        val articleImage = arguments?.getString("articleImage")

        binding.textCategory.text = articleCategory.uppercase()
        binding.textReadTime.text = articleReadTime
        binding.textAuthorName.text = articleAuthor
        binding.textAuthorMeta.text = listOf(articlePublishedAt, getString(R.string.article_updated_placeholder))
            .filter { it.isNotBlank() }
            .joinToString("  •  ")

        binding.imageAuthor.load("https://i.pravatar.cc/150?img=12") {
            crossfade(true)
        }
        binding.imageArticle.load(articleImage) {
            crossfade(true)
            placeholder(R.drawable.bg_home_news_thumb_1)
            error(R.drawable.bg_home_news_thumb_1)
        }

        binding.buttonBack.setOnClickListener {
            findNavController().popBackStack()
        }

        binding.buttonMenu.setOnClickListener {
            Toast.makeText(requireContext(), getString(R.string.home_placeholder_action), Toast.LENGTH_SHORT).show()
        }
        binding.buttonRefresh.setOnClickListener {
            viewModel.loadArticle(articleUrl, articleTitle, articleSource, articlePublishedAt, articleSummary)
        }
        binding.buttonProfileShortcut.setOnClickListener {
            findNavController().navigate(R.id.profileFragment)
        }
        binding.buttonShare.setOnClickListener {
            Toast.makeText(requireContext(), getString(R.string.article_share), Toast.LENGTH_SHORT).show()
        }
        binding.buttonBookmark.setOnClickListener {
            Toast.makeText(requireContext(), getString(R.string.article_bookmark), Toast.LENGTH_SHORT).show()
        }

        binding.navNews.setOnClickListener { }
        binding.navElections.setOnClickListener { findNavController().navigate(R.id.electionsFragment) }
        binding.navProfile.setOnClickListener { findNavController().navigate(R.id.profileFragment) }

        binding.buttonOpenSource.setOnClickListener {
            val url = viewModel.uiState.value?.detail?.articleUrl
            if (url.isNullOrBlank()) return@setOnClickListener
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        }

        viewModel.uiState.observe(viewLifecycleOwner) { state ->
            binding.progressDetail.visibility = if (state.isLoading) View.VISIBLE else View.GONE
            val detail = state.detail
            val resolvedTitle = detail?.title.orEmpty().ifBlank { articleTitle }
            val resolvedSource = detail?.source.orEmpty().ifBlank { articleSource }
            val resolvedDate = detail?.publishedAt.orEmpty().ifBlank { articlePublishedAt }
            val resolvedBody = detail?.content.orEmpty().ifBlank { articleSummary }

            binding.textTitle.text = resolvedTitle
            binding.textAuthorMeta.text = listOf(resolvedDate, getString(R.string.article_updated_placeholder))
                .filter { it.isNotBlank() }
                .joinToString("  •  ")
            binding.textContent.text = resolvedBody
            binding.textPhotoCaption.text = getString(R.string.article_photo_caption_source, resolvedSource)
            binding.buttonOpenSource.isEnabled = !detail?.articleUrl.isNullOrBlank()
            state.messageRes?.let {
                Toast.makeText(requireContext(), getString(it), Toast.LENGTH_SHORT).show()
                viewModel.consumeMessage()
            }
        }

        viewModel.loadArticle(articleUrl, articleTitle, articleSource, articlePublishedAt, articleSummary)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}


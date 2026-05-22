package com.example.newshub.feature.news

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import coil.load
import coil.transform.CircleCropTransformation
import com.example.newshub.R
import com.example.newshub.core.session.AndroidSessionStore
import com.example.newshub.databinding.FragmentNewsDetailBinding
import com.example.newshub.toDetailBundle
import com.example.newshub.ui.BottomNavHelper
import com.example.newshub.ui.LocationNavHelper
import com.example.newshub.ui.ReportBottomSheetFragment

class NewsDetailFragment : Fragment() {

    private var _binding: FragmentNewsDetailBinding? = null
    private val binding get() = _binding!!

    private val viewModel: NewsDetailViewModel by viewModels()
    private val commentsViewModel: CommentsViewModel by viewModels {
        CommentsViewModelFactory(AndroidSessionStore(requireContext().applicationContext))
    }

    private lateinit var commentAdapter: CommentAdapter
    private lateinit var relatedAdapter: RelatedArticlesAdapter

    private val commentPollHandler = Handler(Looper.getMainLooper())
    private val commentPollRunnable = object : Runnable {
        override fun run() {
            val articleId = arguments?.getString("articleId").orEmpty()
            if (articleId.isNotBlank()) {
                commentsViewModel.load(articleId)
            }
            commentPollHandler.postDelayed(this, 30_000L)
        }
    }

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

        val articleId = arguments?.getString("articleId").orEmpty()
        val articleUrl = arguments?.getString("articleUrl")
        val articleTitle = arguments?.getString("articleTitle").orEmpty()
        val articleSource = arguments?.getString("articleSource").orEmpty()
        val articlePublishedAt = arguments?.getString("articlePublishedAt").orEmpty()
        val articleSummary = arguments?.getString("articleSummary").orEmpty()
        val articleCategory = arguments?.getString("articleCategory").orEmpty().ifBlank { "Top Stories" }
        val articleAuthor = arguments?.getString("articleAuthor").orEmpty().ifBlank { "NewsHub" }
        val articleAuthorImage = arguments?.getString("articleAuthorImage")
        val articleReadTime = arguments?.getString("articleReadTime").orEmpty().ifBlank { "3 min read" }
        val articleImage = arguments?.getString("articleImage")

        // Initial UI state from arguments
        binding.textTitle.text = articleTitle.ifBlank { getString(R.string.news_detail_title) }
        binding.textCategory.text = articleCategory.uppercase()
        binding.textReadTime.text = articleReadTime
        binding.textAuthorName.text = articleAuthor.ifBlank { articleSource }.ifBlank { "NewsHub" }
        binding.textAuthorMeta.text = listOf(articlePublishedAt, getString(R.string.article_updated_placeholder))
            .filter { it.isNotBlank() }
            .joinToString("  •  ")

        binding.imageAuthor.load(articleAuthorImage) {
            crossfade(true)
            placeholder(R.drawable.bg_profile_avatar)
            error(R.drawable.bg_profile_avatar)
            transformations(CircleCropTransformation())
        }
        
        binding.imageArticle.load(articleImage) {
            crossfade(true)
            placeholder(R.drawable.bg_home_news_thumb_1)
            error(R.drawable.bg_home_news_thumb_1)
        }

        commentAdapter = CommentAdapter(
            currentUserId = commentsViewModel.currentUserId(),
            onDelete = { item -> commentsViewModel.deleteComment(item.id) },
            onReport = { item -> openReportSheet("COMMENT", item.id, item.content.take(80)) }
        )
        binding.recyclerComments.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerComments.adapter = commentAdapter
        binding.recyclerComments.isNestedScrollingEnabled = false

        relatedAdapter = RelatedArticlesAdapter { article ->
            findNavController().navigate(
                R.id.action_newsDetailFragment_to_newsDetailFragment,
                article.toDetailBundle()
            )
        }
        binding.recyclerRelated.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        binding.recyclerRelated.adapter = relatedAdapter
        binding.recyclerRelated.isNestedScrollingEnabled = false

        binding.buttonBack.setOnClickListener {
            findNavController().popBackStack()
        }

        binding.topBarInclude.buttonSearch.setOnClickListener {
            findNavController().navigate(R.id.searchFragment)
        }
        LocationNavHelper.wirePin(this, binding.topBarInclude) { }

        binding.buttonReportArticle.setOnClickListener {
            val label = binding.textTitle.text?.toString()?.take(120) ?: articleTitle
            val targetId = articleId.ifBlank { articleUrl.orEmpty() }
            openReportSheet("ARTICLE", targetId, label)
        }

        binding.buttonShare.setOnClickListener {
            Toast.makeText(requireContext(), getString(R.string.article_share), Toast.LENGTH_SHORT).show()
        }
        binding.buttonBookmark.setOnClickListener {
            Toast.makeText(requireContext(), getString(R.string.article_bookmark), Toast.LENGTH_SHORT).show()
        }

        BottomNavHelper.wire(
            fragment = this,
            navHome = binding.bottomNavBar.navHome,
            navElections = binding.bottomNavBar.navElections,
            navAlerts = binding.bottomNavBar.navAlerts,
            navProfile = binding.bottomNavBar.navProfile
        )

        binding.buttonOpenSource.setOnClickListener {
            val url = viewModel.uiState.value?.detail?.articleUrl
            if (url.isNullOrBlank()) return@setOnClickListener
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        }

        binding.inputComment.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
            override fun afterTextChanged(s: Editable?) {
                binding.buttonPostComment.isEnabled = !s.isNullOrBlank()
            }
        })

        binding.buttonPostComment.setOnClickListener {
            val text = binding.inputComment.text?.toString().orEmpty()
            commentsViewModel.postComment(text)
            binding.inputComment.text?.clear()
        }

        viewModel.uiState.observe(viewLifecycleOwner) { state ->
            binding.progressDetail.visibility = if (state.isLoading) View.VISIBLE else View.GONE
            val detail = state.detail
            if (detail != null) {
                val resolvedTitle = detail.title.ifBlank { articleTitle }
                val resolvedAuthor = detail.author?.ifBlank { null } ?: detail.source.ifBlank { articleAuthor }
                val resolvedDate = detail.publishedAt.ifBlank { articlePublishedAt }
                val resolvedBody = detail.content.ifBlank { articleSummary }

                binding.textTitle.text = resolvedTitle.ifBlank { getString(R.string.news_detail_title) }
                binding.textAuthorName.text = resolvedAuthor
                
                if (!detail.authorImageUrl.isNullOrBlank()) {
                    binding.imageAuthor.load(detail.authorImageUrl) {
                        crossfade(true)
                        placeholder(R.drawable.bg_profile_avatar)
                        error(R.drawable.bg_profile_avatar)
                        transformations(CircleCropTransformation())
                    }
                }

                binding.textAuthorMeta.text = listOf(resolvedDate, getString(R.string.article_updated_placeholder))
                    .filter { it.isNotBlank() }
                    .joinToString("  •  ")
                binding.textContent.text = resolvedBody
                binding.textPhotoCaption.text = getString(R.string.article_photo_caption_source, detail.source.ifBlank { "NewsHub" })
                binding.buttonOpenSource.isEnabled = !detail.articleUrl.isNullOrBlank()
            }
            
            relatedAdapter.submitList(state.relatedArticles)
            state.messageRes?.let {
                Toast.makeText(requireContext(), getString(it), Toast.LENGTH_SHORT).show()
                viewModel.consumeMessage()
            }
        }

        commentsViewModel.uiState.observe(viewLifecycleOwner) { state ->
            binding.textCommentsHeader.text = getString(R.string.comments_header, state.comments.size)
            commentAdapter.submitList(state.comments)
            
            // Update the user's avatar in the "Share your thoughts" input
            updateCommentInputAvatar(state.currentUserAvatar, state.currentUserFullName)

            state.messageRes?.let {
                Toast.makeText(requireContext(), getString(it), Toast.LENGTH_SHORT).show()
                commentsViewModel.consumeMessage()
            }
        }

        viewModel.loadArticle(
            url = articleUrl,
            fallbackTitle = articleTitle,
            fallbackSource = articleSource,
            fallbackAuthor = articleAuthor,
            fallbackAuthorImageUrl = articleAuthorImage,
            fallbackPublishedAt = articlePublishedAt,
            fallbackSummary = articleSummary
        )
        viewModel.loadRelated(articleCategory, articleId)
        if (articleId.isNotBlank()) {
            commentsViewModel.load(articleId)
        }
    }

    private fun updateCommentInputAvatar(url: String?, fullName: String?) {
        if (!url.isNullOrBlank()) {
            binding.imageCommentAvatar.load(url) {
                crossfade(true)
                placeholder(R.drawable.bg_auth_logo)
                error(R.drawable.bg_auth_logo)
                transformations(CircleCropTransformation())
            }
            binding.textCommentAvatarInitials.visibility = View.GONE
        } else {
            binding.imageCommentAvatar.setImageResource(R.drawable.bg_auth_logo)
            val initials = fullName?.trim()?.split(" ")
                ?.mapNotNull { it.firstOrNull()?.uppercaseChar() }
                ?.take(2)
                ?.joinToString("")
                ?.ifBlank { "NH" } ?: "NH"
            binding.textCommentAvatarInitials.text = initials
            binding.textCommentAvatarInitials.visibility = View.VISIBLE
        }
    }

    private fun openReportSheet(targetType: String, targetId: String, targetLabel: String?) {
        if (targetId.isBlank()) {
            Toast.makeText(requireContext(), R.string.report_failed, Toast.LENGTH_SHORT).show()
            return
        }
        ReportBottomSheetFragment.newInstance(targetType, targetId, targetLabel)
            .show(parentFragmentManager, "report_sheet")
    }

    override fun onResume() {
        super.onResume()
        commentPollHandler.postDelayed(commentPollRunnable, 30_000L)
    }

    override fun onPause() {
        commentPollHandler.removeCallbacks(commentPollRunnable)
        super.onPause()
    }

    override fun onDestroyView() {
        commentPollHandler.removeCallbacks(commentPollRunnable)
        super.onDestroyView()
        _binding = null
    }
}

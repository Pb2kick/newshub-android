package com.example.newshub.feature.home

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.newshub.NewsArticle
import com.example.newshub.R
import com.example.newshub.databinding.ItemNewsArticleBinding

class NewsAdapter(
    private val onClick: (NewsArticle) -> Unit
) : ListAdapter<NewsArticle, NewsAdapter.NewsViewHolder>(Diff) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NewsViewHolder {
        val binding = ItemNewsArticleBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return NewsViewHolder(binding, onClick)
    }

    override fun onBindViewHolder(holder: NewsViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class NewsViewHolder(
        private val binding: ItemNewsArticleBinding,
        private val onClick: (NewsArticle) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: NewsArticle) {
            binding.imageThumbnail.load(item.imageUrl) {
                crossfade(true)
                placeholder(R.drawable.bg_home_news_thumb_1)
                error(R.drawable.bg_home_news_thumb_1)
            }
            binding.textCategory.text = item.category
            binding.textTitle.text = item.title
            binding.textSummary.text = item.summary
            binding.textMeta.text = listOf(item.author, item.publishedAt)
                .filter { it.isNotBlank() }
                .joinToString(" • ")
            binding.textReadTime.text = item.readTime
            binding.root.setOnClickListener { onClick(item) }
        }
    }

    private object Diff : DiffUtil.ItemCallback<NewsArticle>() {
        override fun areItemsTheSame(oldItem: NewsArticle, newItem: NewsArticle): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: NewsArticle, newItem: NewsArticle): Boolean {
            return oldItem == newItem
        }
    }
}


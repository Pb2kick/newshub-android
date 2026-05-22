package com.example.newshub.feature.news

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.newshub.NewsArticle
import com.example.newshub.R
import com.example.newshub.databinding.ItemRelatedArticleBinding

class RelatedArticlesAdapter(
    private val onClick: (NewsArticle) -> Unit
) : ListAdapter<NewsArticle, RelatedArticlesAdapter.RelatedViewHolder>(Diff) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RelatedViewHolder {
        val binding = ItemRelatedArticleBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return RelatedViewHolder(binding, onClick)
    }

    override fun onBindViewHolder(holder: RelatedViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class RelatedViewHolder(
        private val binding: ItemRelatedArticleBinding,
        private val onClick: (NewsArticle) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: NewsArticle) {
            binding.imageThumbnail.load(item.imageUrl) {
                crossfade(true)
                placeholder(R.drawable.bg_home_news_thumb_1)
                error(R.drawable.bg_home_news_thumb_1)
            }
            binding.textTitle.text = item.title
            binding.textMeta.text = listOf(item.source, item.publishedAt)
                .filter { it.isNotBlank() }
                .joinToString(" • ")
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

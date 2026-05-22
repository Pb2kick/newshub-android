package com.example.newshub

import android.os.Bundle
fun NewsArticle.toDetailBundle(): Bundle {
    return Bundle().apply {
        putString("articleId", id)
        putString("articleUrl", articleUrl)
        putString("articleTitle", title)
        putString("articleSource", source)
        putString("articlePublishedAt", publishedAt)
        putString("articleSummary", summary)
        putString("articleCategory", category)
        putString("articleAuthor", author)
        putString("articleAuthorImage", authorImageUrl)
        putString("articleReadTime", readTime)
        putString("articleImage", imageUrl)
    }
}

fun ElectionRecord.toDetailBundle(): Bundle {
    return Bundle().apply {
        putString("electionId", com.example.newshub.core.RestIdNormalizer.normalize(id))
        putString("electionName", name)
        putString("electionStatus", status)
        putString("electionStartDate", startDate)
        putString("electionEndDate", endDate)
        putString("electionDescription", description)
        putString("electionRegion", region)
        putString("electionImageUrl", imageUrl)
        putInt("electionCandidateCount", candidateCount)
    }
}

fun CandidateRecord.toProfileBundle(electionName: String): Bundle {
    return Bundle().apply {
        putString("candidateId", com.example.newshub.core.RestIdNormalizer.normalize(id))
        putString("electionId", com.example.newshub.core.RestIdNormalizer.normalize(electionId))
        putString("electionName", electionName)
        putString("candidateName", fullName)
        putString("candidateParty", party)
        putString("candidatePlatform", platform)
        putString("candidatePhotoUrl", photoUrl)
        putString("candidatePosition", position)
        putString("candidateEducation", education)
    }
}

fun CandidateRecord.toVoteBundle(electionName: String): Bundle {
    return Bundle().apply {
        putString("electionId", com.example.newshub.core.RestIdNormalizer.normalize(electionId))
        putString("electionName", electionName)
        putString("candidateId", com.example.newshub.core.RestIdNormalizer.normalize(id))
        putString("candidateName", fullName)
    }
}

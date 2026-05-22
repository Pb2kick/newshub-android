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
        putString("articleReadTime", readTime)
        putString("articleImage", imageUrl)
    }
}

fun ElectionRecord.toDetailBundle(): Bundle {
    return Bundle().apply {
        putString("electionId", id)
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
        putString("candidateId", id)
        putString("electionId", electionId)
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
        putString("electionId", electionId)
        putString("electionName", electionName)
        putString("candidateId", id)
        putString("candidateName", fullName)
    }
}

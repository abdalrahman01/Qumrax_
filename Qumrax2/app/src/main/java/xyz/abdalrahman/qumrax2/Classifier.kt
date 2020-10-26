package xyz.abdalrahman.qumrax2

import android.graphics.RectF


interface Classifier {
    /** An immutable result returned by a Classifier describing what was recognized.  */
    class Recognition(
        /**
         * A unique identifier for what has been recognized. Specific to the class, not the instance of
         * the object.
         */
        private val id: String,
        /** Display name for the recognition.  */
        val title: String,
        /**
         * A sortable score for how good the recognition is relative to others. Higher should be better.
         */
        val confidence: Float,
        /** Optional location within the source image for the location of the recognized object.  */
        private var location: RectF
    ) {
        fun getLocation(): RectF = RectF(location)
        fun setLocation(rectF: RectF) {
            location = rectF
        }
        fun getTitle_(): String = title
        fun getScore(): Float = confidence
        fun getRoundedScore(): Double = (confidence * 100).toDouble()
        override fun toString(): String {
            return "[$id] $title ${confidence * 100.0f} $location"
        }
    }
}
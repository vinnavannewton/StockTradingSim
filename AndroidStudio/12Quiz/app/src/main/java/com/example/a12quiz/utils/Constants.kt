package com.example.a12quiz.utils

import android.widget.Toast
import com.example.a12quiz.R
import com.example.a12quiz.model.Question

object Constants {
    fun getQuestions(): MutableList<Question> {
        val questions = mutableListOf<Question>()

        val quest1 = Question(
            1, "Which one of the species does this animal belong to?",
            R.drawable.animal,
            "Pig",
            "Rat",
            "Dog",
            "Vinnavan's pet cat ",
            4
        )
        questions.add(quest1)

        val quest2 = Question(
            2, "When is your boyfriend's birthday?(hint:Main BF not side)",
            R.drawable.two_hands,
            "29-05-2006",
            "28-04-2006",
            "29-04-2006",
            "04-03-2006",
            3
        )
        questions.add(quest2)

        val quest3 = Question(
            3, "My most favorite car?",
            R.drawable.two_hands,
            "Lamborghini",
            "Ferrari",
            "toyota gr series",
            "porche",
            3
        )
        questions.add(quest3)

        val quest4 = Question(
            4, "Where was i born?",
            R.drawable.two_hands,
            "Bangalore",
            "Madurai",
            "KGF",
            "Bangarpet",
            1
        )
        questions.add(quest4)

        val quest5 = Question(
            5, "Did i make u day more meow's or more bow's?",
            R.drawable.two_hands,
            "Bow's",
            "Meow's",
            "None",
            "Both are equal!",
            2
        )
        questions.add(quest5)

        val quest6 = Question(
            6, "What's my favorite car video game?",
            R.drawable.two_hands,
            "Forza Horizon4",
            "Gran Turismo",
            "Beamng.drive",
            "Isle of man",
            3
        )
        questions.add(quest6)

        val quest7 = Question(
            7, "My favorite flavour?",
            R.drawable.two_hands,
            "Chocolate",
            "Belgium chocolate",
            "Mango",
            "Your \uD83D\uDE3A",
            4
        )
        questions.add(quest7)

        val quest8 = Question(
            8, "My favorite color?",
            R.drawable.two_hands,
            "Red",
            "Green",
            "Cyan",
            "Blue",
            2
        )
        questions.add(quest8)

        val quest9 = Question(
            9, "My most favorite GF",
            R.drawable.sshh,
            "ChatGPT",
            "Claude",
            "Lenovo",
            "Ankita(neetha naaye)",
            4
        )
        questions.add(quest9)

        val quest10 = Question(
            10, "Will you marry me?",
            R.drawable.two_hands,
            "Yes",
            "Maybe",
            "No",
            "Never",
            1
        )
        questions.add(quest10)


        return questions //at last question put this.
    }
}
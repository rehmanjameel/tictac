package io.xconn.tictackotlin

import android.animation.Animator
import android.graphics.Color
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.util.TypedValue
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.GridLayout
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import io.xconn.tictackotlin.App.Companion.session
import io.xconn.tictackotlin.databinding.ActivityMainBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Random
import kotlin.math.max
import kotlin.math.min


class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    private var gridLayout: GridLayout? = null
    private var gameActive = true
    private var mediaPlayer: MediaPlayer? = null
    private var loseMediaPlayer: MediaPlayer? = null
    private var gridSize = 3 // Change to 5 or 7 for larger boards
    private var isOnlineGame = true;
    private var isMyTurn = true  // Track whose turn it is

    private val app = App()
    val userId = app.getValueInt("user_id")
    var secondPlayerId: Int = 0
    private var wonCount = 0

    // Player representation
    // 0 - X
    // 1 - O
    private var activePlayer = 0
    private lateinit var gameState: Array<IntArray>
    private var isRobotEnabled = false // Default: Two Players mode

    private var delayTime = 350

    private var counter = 0
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        gridLayout = binding.gridLayouts

        // get second player id
        // Get the user_id as a String first
        val userIdString = intent.getStringExtra("user_id")

        // Convert it to Int safely
        secondPlayerId = userIdString?.toIntOrNull() ?: 0
        Log.e("user_id of player", secondPlayerId.toString())

        val gameLevelType = intent.getStringExtra("level_type")
        val personType = intent.getStringExtra("person_type")
        if (gameLevelType != null && gameLevelType == "easy_level") {
            binding.levelText.text = "Easy"
            binding.levelImage.setImageResource(R.drawable.easy3)
            if (personType != null && personType == "p2r") {
                isRobotEnabled = true
                binding.secondPlayerText.text = "AI Robot"
                binding.secondPlayerImage.setImageResource(R.drawable.robot_assistant)
            }

            if (personType != null && personType == "p2p") {
                isRobotEnabled = false
                binding.secondPlayerImage.setImageResource(R.drawable.circle)
            }
            gridSize = 3
        } else if (gameLevelType != null && gameLevelType == "medium_level") {
            binding.levelText.text = "Medium"
            binding.levelImage.setImageResource(R.drawable.medium4)

            gridSize = 5
            if (personType != null && personType == "p2r") {
                isRobotEnabled = true
                delayTime = 350
                binding.secondPlayerText.text = "AI Robot"

                binding.secondPlayerImage.setImageResource(R.drawable.robot_assistant)
            }

            if (personType != null && personType == "p2p") {
                isRobotEnabled = false
                binding.secondPlayerImage.setImageResource(R.drawable.circle)
            }
        } else if (gameLevelType != null && gameLevelType == "hard_level") {
            binding.levelText.text = "Hard"
            binding.levelImage.setImageResource(R.drawable.hard5)

            gridSize = 7
            if (personType != null && personType == "p2r") {
                isRobotEnabled = true
                delayTime = 350
                binding.secondPlayerImage.setImageResource(R.drawable.robot_assistant)
                binding.secondPlayerText.text = "AI Robot"
            }

            if (personType != null && personType == "p2p") {
                isRobotEnabled = false
                binding.secondPlayerImage.setImageResource(R.drawable.circle)
            }
        }

        binding.titleTextView.setShadowLayer(10F, 5F, 5F, ContextCompat.getColor(this, R.color.oColor))
        binding.statusTextView.setShadowLayer(
            10F,
            5F,
            5F,
            ContextCompat.getColor(this, R.color.oColor)
        )

        initializeBoard(gridSize)

        // Set initial status
        binding.statusTextView.text = "X's Turn"


        // Set restart button listener
        binding.restartButton.setOnClickListener { v -> initializeBoard(gridSize) }


        // Set gridlayout to restart the game
        binding.gridLayout.setOnClickListener { view -> initializeBoard(gridSize) }


        // Load sound
        mediaPlayer = MediaPlayer.create(this, R.raw.hurrah) // Place sound in res/raw

        loseMediaPlayer = MediaPlayer.create(this, R.raw.losing)


        // Obtain the FirebaseAnalytics instance.
//        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this)
//        val bundle = Bundle()
//        bundle.putString(FirebaseAnalytics.Param.ITEM_ID, "id")
//        bundle.putString(FirebaseAnalytics.Param.ITEM_NAME, "name")
//        bundle.putString(FirebaseAnalytics.Param.CONTENT_TYPE, "image")
//        mFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, bundle)

        binding.backButton.setOnClickListener { view ->
            onBackPressed()
        }

    }

    private suspend fun subscribeUser() {
        session.subscribe("io.xconn.tictac.$userId", { event ->
            Log.e("user subscribed;", event.args.toString() + userId)
            Log.e("user subscribed00;", event.args?.size.toString() + userId)

            // Extracting values from the list
            val row = event.args?.getOrNull(0) as? Int
            val col = event.args?.getOrNull(1) as? Int
            val isWin = event.args?.getOrNull(2) as? Boolean

            if (row != null && col != null && isWin != null) {
                Log.e("Move Received:", "Row: $row, Col: $col, isWin: $isWin")

                runOnUiThread {
                    val img = getImageViewByTag(row, col)
                    if (gameState[row][col] == 2) {
                        gameState[row][col] = activePlayer
                        img.setImageResource(if (activePlayer == 0) R.drawable.x else R.drawable.o)
                        img.imageTintList = ContextCompat.getColorStateList(
                            this, if (activePlayer == 0) R.color.xColor else R.color.oColor
                        )
                        img.translationY = -1000f
                        img.animate().translationYBy(1000f).setDuration(300).start()

                        activePlayer = 1 - activePlayer  // Switch turn
                        isMyTurn = true  // Allow the player to play now

                        binding.statusTextView.text = "Your Turn!"
                    }

                    if (isWin) {
                        if (isBoardFull()) {
                            binding.statusTextView.text = "It's a Draw!"
                            showWinLosePopUp("OOO!!!", "It's a Draw!")
                            loseMediaPlayer?.start()
                        } else {
                            isWinningMove()  // Handle win
                        }
                    }
                }
            } else {
                Log.e("Move Error", "Invalid move received")
            }
        })
    }


    override fun onResume() {
        super.onResume()
        lifecycleScope.launch {
            subscribeUser()
        }
    }

    private fun initializeBoard(size: Int) {
        gridSize = size
        gameActive = true
        activePlayer = 0
        counter = 0
        binding.statusTextView.text = "X's Turn"

        gridLayout!!.removeAllViews()
        gridLayout!!.columnCount = gridSize
        gridLayout!!.rowCount = gridSize

        gameState = Array(gridSize) { IntArray(gridSize) }

        // Fixed GridLayout width and height in dp
        val totalGridWidthDp = 350 // Full grid width including margins
        val totalGridHeightDp = 400 // Full grid height including margins
        val cellAreaWidthDp = 315 // **Only for cells, margins will use remaining space**
        val cellAreaHeightDp = 370 // **Height reserved for cells**
        val marginDp = 2 // Small margin

        // Convert dp to pixels
        val totalGridWidthPx = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            totalGridWidthDp.toFloat(),
            resources.displayMetrics
        ).toInt()
        val totalGridHeightPx = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            totalGridHeightDp.toFloat(),
            resources.displayMetrics
        ).toInt()
        val cellAreaWidthPx = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            cellAreaWidthDp.toFloat(),
            resources.displayMetrics
        ).toInt()
        val cellAreaHeightPx = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            cellAreaHeightDp.toFloat(),
            resources.displayMetrics
        ).toInt()
        val marginPx = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            marginDp.toFloat(),
            resources.displayMetrics
        ).toInt()

        // Apply fixed dimensions to GridLayout and center it
        val gridParams = gridLayout!!.layoutParams
        gridParams.width = totalGridWidthPx
        gridParams.height = totalGridHeightPx
        gridLayout!!.layoutParams = gridParams

        // **Fix both column & row overflow issues**:
        val totalMarginWidth = (gridSize - 1) * marginPx
        val totalMarginHeight = (gridSize - 1) * marginPx
        val cellWidth = (cellAreaWidthPx - totalMarginWidth) / gridSize
        val cellHeight =
            (cellAreaHeightPx - totalMarginHeight) / gridSize // Now height is also fixed properly

        for (i in 0 until gridSize) {
            for (j in 0 until gridSize) {
                gameState[i][j] = 2

                val cell = ImageView(this)
                cell.adjustViewBounds = true
                cell.setPadding(2, 2, 2, 2)
                cell.scaleType = ImageView.ScaleType.CENTER_CROP
                cell.setImageResource(android.R.color.transparent)
                cell.setBackgroundColor(Color.WHITE)

                val params = GridLayout.LayoutParams()
                params.width = cellWidth
                params.height = cellHeight
                params.setMargins(marginPx, marginPx, marginPx, marginPx)
                cell.layoutParams = params

                val finalI = i
                val finalJ = j
                cell.setOnClickListener { v: View? ->
                    playerTap(
                        finalI,
                        finalJ,
                        cell
                    )
                }

                gridLayout!!.addView(cell)
            }
        }
    }

    private suspend fun publishMove(row: Int, col: Int, isWin: Boolean) {
        val data = listOf(row, col, isWin) // Send values directly

        session.publish("io.xconn.tictac.$secondPlayerId", data)
    }


    private fun generateWinPositions(gridSize: Int): List<IntArray> {
        val winPositions: MutableList<IntArray> = ArrayList()

        val winCondition = if ((gridSize == 5)) 4 else if ((gridSize == 7)) 5
        else if ((gridSize == 9)) 6 else gridSize // 4 for 5x5, 5 for 7x7, full for 3x3

        // Rows
        for (row in 0 until gridSize) {
            for (start in 0..gridSize - winCondition) {
                val winRow = IntArray(winCondition)
                for (i in 0 until winCondition) {
                    winRow[i] = row * gridSize + (start + i)
                }
                winPositions.add(winRow)
            }
        }

        // Columns
        for (col in 0 until gridSize) {
            for (start in 0..gridSize - winCondition) {
                val winColumn = IntArray(winCondition)
                for (i in 0 until winCondition) {
                    winColumn[i] = (start + i) * gridSize + col
                }
                winPositions.add(winColumn)
            }
        }

        // Primary Diagonal (\ direction)
        for (row in 0..gridSize - winCondition) {
            for (col in 0..gridSize - winCondition) {
                val primaryDiagonal = IntArray(winCondition)
                for (i in 0 until winCondition) {
                    primaryDiagonal[i] = (row + i) * gridSize + (col + i)
                }
                winPositions.add(primaryDiagonal)
            }
        }

        // Secondary Diagonal (/ direction)
        for (row in 0..gridSize - winCondition) {
            for (col in winCondition - 1 until gridSize) {
                val secondaryDiagonal = IntArray(winCondition)
                for (i in 0 until winCondition) {
                    secondaryDiagonal[i] = (row + i) * gridSize + (col - i)
                }
                winPositions.add(secondaryDiagonal)
            }
        }

        return winPositions
    }

    // method to apply the players X or O in board
    private fun playerTap(row: Int, col: Int, img: ImageView) {
        if (!gameActive || gameState[row][col] != 2 || !isMyTurn) return  // Block if game over

        // Update state
        gameState[row][col] = activePlayer
        counter++

        if (activePlayer == 0) {
            img.setImageResource(R.drawable.x)
            img.imageTintList = ContextCompat.getColorStateList(this, R.color.xColor)
        } else {
            img.setImageResource(R.drawable.o)
            img.imageTintList = ContextCompat.getColorStateList(this, R.color.oColor)
        }

        img.translationY = -1000f
        img.animate().translationYBy(1000f).setDuration(300).start()

        // **Check if this move is a winning move**
        val isWin = isWinningMove()

        // **Check for a Draw AFTER checking for a win**
        if (!isWin && isBoardFull()) {
            binding.statusTextView.text = "It's a Draw!"
            showWinLosePopUp("OOO!!!", "It's a Draw!")
            loseMediaPlayer?.start()
            gameActive = false

            // **Send Draw Info to Other Player**
            if (isOnlineGame) {
                CoroutineScope(Dispatchers.IO).launch {
                    publishMove(row, col, true)  // Send move with `isWin = true` to indicate game over
                }
            }

            return  // **Exit function to prevent further moves**
        }

        // **Send Move to Other Player BEFORE stopping game**
        if (isOnlineGame) {
            CoroutineScope(Dispatchers.IO).launch {
                publishMove(row, col, isWin)  // Send move and win info
            }
        }

        if (isWin) return  // **Stop further moves if game is over**

        activePlayer = 1 - activePlayer  // Switch turns
        isMyTurn = false  // Prevent another move until opponent moves

        binding.statusTextView.text = "Waiting for Opponent..."
    }


    private fun isBoardFull(): Boolean {
        for (row in gameState) {
            if (row.contains(2)) return false  // If any cell is empty, the game is not a draw
        }
        return true
    }


    private fun isWinningMove(): Boolean {
        val winPositions = generateWinPositions(gridSize)

        for (winPosition in winPositions) {
            val firstCell = gameState[winPosition[0] / gridSize][winPosition[0] % gridSize]
            var win = true

            for (i in 1 until winPosition.size) {
                val r = winPosition[i] / gridSize
                val c = winPosition[i] % gridSize
                if (gameState[r][c] != firstCell || firstCell == 2) {
                    win = false
                    break
                }
            }

            if (win) {
                gameActive = false  // **Stop the game**
                highlightWinningCells(winPosition)
                applyWinAnimation()
                showHurrahAnimation()
                return true  // **Return true if game is won**
            }
        }
        return false
    }


    private fun aiMove() {
        val emptyCells: MutableList<IntArray> = ArrayList()

        for (i in 0 until gridSize) {
            for (j in 0 until gridSize) {
                if (gameState[i][j] == 2) {
                    emptyCells.add(intArrayOf(i, j))
                }
            }
        }

        if (!emptyCells.isEmpty()) {
            val move = emptyCells[Random().nextInt(emptyCells.size)]
            //            playerTap(move[0], move[1], getImageViewByTag(move[0] * gridSize + move[1]));
        }
    }

    private fun minimax(board: Array<IntArray>, depth: Int, isMax: Boolean): Int {
        val score = evaluateBoard(board)

        // Base cases: Win, Loss, or Draw
        if (score == 10) return score - depth // Prefer faster wins

        if (score == -10) return score + depth // Prefer slower losses

        if (!isMovesLeft(board)) return 0 // Draw


        // **Limit depth for larger boards to avoid lag**
        val maxDepth = if ((gridSize == 3)) 9 else if ((gridSize == 5)) 4 else 3
        if (depth >= maxDepth) return 0 // Stop recursion


        if (isMax) {  // Maximizing (AI's turn)
            var best = -1000
            for (i in 0 until gridSize) {
                for (j in 0 until gridSize) {
                    if (board[i][j] == 2) {
                        board[i][j] = 0
                        best = max(best.toDouble(), minimax(board, depth + 1, false).toDouble())
                            .toInt()
                        board[i][j] = 2
                    }
                }
            }
            return best
        } else {  // Minimizing (Player's turn)
            var best = 1000
            for (i in 0 until gridSize) {
                for (j in 0 until gridSize) {
                    if (board[i][j] == 2) {
                        board[i][j] = 1
                        best = min(best.toDouble(), minimax(board, depth + 1, true).toDouble())
                            .toInt()
                        board[i][j] = 2
                    }
                }
            }
            return best
        }
    }


    private fun findBestMove(board: Array<IntArray>): IntArray {
        var bestVal = -1000
        var bestRow = -1
        var bestCol = -1

        // Loop through all cells, evaluate each move, and pick the best
        for (i in 0 until gridSize) {
            for (j in 0 until gridSize) {
                if (board[i][j] == 2) { // If empty
                    board[i][j] = 0 // AI makes move
                    val moveVal = minimax(board, 0, false)
                    board[i][j] = 2 // Undo move

                    if (moveVal > bestVal) {
                        bestRow = i
                        bestCol = j
                        bestVal = moveVal
                    }
                }
            }
        }

        return intArrayOf(bestRow, bestCol)
    }


    private fun getImageViewByTag(row: Int, col: Int): ImageView {
        val index = row * gridSize + col // Convert (row, col) to 1D index
        return gridLayout!!.getChildAt(index) as ImageView
    }


    private fun evaluateBoard(board: Array<IntArray>): Int {
        val winCondition =
            if ((gridSize == 5)) 4 else if ((gridSize == 7)) 5 else gridSize // 4 in a row for 5x5, 5 for 7x7

        // Check Rows
        for (row in 0 until gridSize) {
            for (start in 0..gridSize - winCondition) {
                var xWin = true
                var oWin = true
                for (i in 0 until winCondition) {
                    if (board[row][start + i] != 0) xWin = false
                    if (board[row][start + i] != 1) oWin = false
                }
                if (xWin) return 10 // AI wins

                if (oWin) return -10 // Player wins
            }
        }

        // Check Columns
        for (col in 0 until gridSize) {
            for (start in 0..gridSize - winCondition) {
                var xWin = true
                var oWin = true
                for (i in 0 until winCondition) {
                    if (board[start + i][col] != 0) xWin = false
                    if (board[start + i][col] != 1) oWin = false
                }
                if (xWin) return 10
                if (oWin) return -10
            }
        }

        // Check Primary Diagonals (\ direction)
        for (row in 0..gridSize - winCondition) {
            for (col in 0..gridSize - winCondition) {
                var xWin = true
                var oWin = true
                for (i in 0 until winCondition) {
                    if (board[row + i][col + i] != 0) xWin = false
                    if (board[row + i][col + i] != 1) oWin = false
                }
                if (xWin) return 10
                if (oWin) return -10
            }
        }

        // Check Secondary Diagonals (/ direction)
        for (row in 0..gridSize - winCondition) {
            for (col in winCondition - 1 until gridSize) {
                var xWin = true
                var oWin = true
                for (i in 0 until winCondition) {
                    if (board[row + i][col - i] != 0) xWin = false
                    if (board[row + i][col - i] != 1) oWin = false
                }
                if (xWin) return 10
                if (oWin) return -10
            }
        }

        return 0 // No winner yet
    }


    private fun isMovesLeft(board: Array<IntArray>): Boolean {
        for (i in 0 until gridSize) for (j in 0 until gridSize) if (board[i][j] == 2) return true
        return false
    }


    private fun highlightWinningCells(winPosition: IntArray) {
        for (pos in winPosition) {
            val row = pos / gridSize
            val col = pos % gridSize
            val img = gridLayout!!.getChildAt(row * gridSize + col) as ImageView
            img.setBackgroundColor(ContextCompat.getColor(this, R.color.winnerColor))
        }
    }

    private fun applyWinAnimation() {
        val winAnim = AnimationUtils.loadAnimation(this, R.anim.win_animation)

        for (row in gameState.indices) {
            for (col in gameState[row].indices) {
                if (gameState[row][col] != 2) { // Check if cell is occupied
                    val position = row * gridSize + col // Convert 2D index to 1D position
                    val img = gridLayout!!.getChildAt(position) as ImageView
                    img?.startAnimation(winAnim)
                }
            }
        }
    }

    private fun showHurrahAnimation() {
        binding.hurrahAnimation.setVisibility(View.VISIBLE)
        binding.hurrahAnimation.playAnimation()

        if (mediaPlayer != null) {
            mediaPlayer!!.start()
        }

        // Hide animation after completion
        binding.hurrahAnimation.addAnimatorListener(object : Animator.AnimatorListener {
            override fun onAnimationStart(animator: Animator) {
            }

            override fun onAnimationEnd(animator: Animator) {
                binding.hurrahAnimation.setVisibility(View.GONE)
            }

            override fun onAnimationCancel(animator: Animator) {
            }

            override fun onAnimationRepeat(animator: Animator) {
            }
        })
    }

    private fun showWinLosePopUp(title: String, message: String) {
        MaterialAlertDialogBuilder(this@MainActivity, R.style.AlertDialogTheme)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton(
                "Play again"
            ) { dialogInterface, i -> initializeBoard(gridSize) }
            .setNeutralButton(
                "Go Back"
            ) { dialogInterface, i -> onBackPressed() }
            .show()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (mediaPlayer != null) {
            mediaPlayer!!.release()
            mediaPlayer = null
        }

        if (loseMediaPlayer != null) {
            loseMediaPlayer!!.release()
            loseMediaPlayer = null
        }
    }

}
const statusText = document.getElementById('status');
const resetBtn = document.getElementById('reset');
const cells = Array.from(document.querySelectorAll('.cell'));

const winningCombos = [
  [0, 1, 2], [3, 4, 5], [6, 7, 8],
  [0, 3, 6], [1, 4, 7], [2, 5, 8],
  [0, 4, 8], [2, 4, 6],
];

let boardState = Array(9).fill('');
let currentPlayer = 'X';
let gameActive = true;

function handleCellClick(event) {
  const index = Number(event.target.dataset.index);
  if (!gameActive || boardState[index]) return;

  boardState[index] = currentPlayer;
  event.target.textContent = currentPlayer;
  event.target.classList.add(currentPlayer.toLowerCase());

  if (checkWin()) {
    statusText.textContent = `Player ${currentPlayer} wins!`;
    gameActive = false;
    return;
  }

  if (boardState.every((cell) => cell)) {
    statusText.textContent = "It's a draw!";
    gameActive = false;
    return;
  }

  currentPlayer = currentPlayer === 'X' ? 'O' : 'X';
  statusText.textContent = `Player ${currentPlayer}'s turn`;
}

function checkWin() {
  return winningCombos.some((combo) => {
    if (combo.every((i) => boardState[i] === currentPlayer)) {
      combo.forEach((i) => cells[i].classList.add('win'));
      return true;
    }
    return false;
  });
}

function resetGame() {
  boardState = Array(9).fill('');
  currentPlayer = 'X';
  gameActive = true;
  statusText.textContent = "Player X's turn";
  cells.forEach((cell) => {
    cell.textContent = '';
    cell.classList.remove('x', 'o', 'win');
  });
}

cells.forEach((cell) => cell.addEventListener('click', handleCellClick));
resetBtn.addEventListener('click', resetGame);

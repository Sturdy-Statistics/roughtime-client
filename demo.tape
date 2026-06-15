# script to make a demo video
#
# see https://github.com/charmbracelet/vhs/tree/main/examples
# run `vhs demo.tape`

# basic config
Set Shell zsh

Set Width 800
# Increased height slightly to give the output a bit more breathing room
Set Height 600

Set FontSize 12
Set FontFamily "Menlo"
Set Theme "iTerm2 Solarized Dark"

Set Margin 24
Set MarginFill "#ACD7C5"

Set WindowBar Colorful
Set Padding 18
Set BorderRadius 12

Set TypingSpeed 55ms
Set Framerate 24

Output out.mp4
Output out.gif

##########################################################################

# Type the comment, pause so the user can read it, then clear the line
Type "# look up a server by name..."
Sleep 1.5s
Ctrl+U

# Type a command in the terminal (using single quotes so double quotes work inside)
Type 'clj -M:run :server-name "SturdyStatistics"'

# Pause before running 
Sleep 700ms
Enter

# Wait for regex to appear
Wait+Screen /skew/

# save a screenshot
Screenshot examples/example.png

# Wait to show output
Sleep 3s

# CLEAR THE SCREEN so the next Wait+Screen doesn't trigger early!
Ctrl+L
Sleep 500ms

##########################################################################

Type "# provide explicit server details..."
Sleep 1.5s
Ctrl+U

Type 'clj -M:run :version-no "0x8000000c" :public-key "AW5uAoTSTDfG5NfY1bTh08GUnOqlRb+HVhbJ3ODJvsE=" :address "roughtime.int08h.com:2002"'

Sleep 700ms
Enter

Wait+Screen /skew/
Sleep 4s

Ctrl+L
Sleep 500ms

##########################################################################

Type "# pretty-print request and response"
Sleep 1.5s
Ctrl+U

# Pipe the output to `less` so we can control the scrolling
Type 'clj -M:run :server-name "SturdyStatistics" :print-request :print-response | less'

Sleep 700ms
Enter

# Wait for the pager to render the first block
Wait+Screen /==== REQUEST ====/
Sleep 2s

# # Scroll down slowly (5 times, waiting 500ms between each press)
# Down@500ms 5
# Sleep 1s

# Scroll down a whole page (Spacebar in less)
Space
Sleep 2s

# Jump to the very bottom of the output (Shift+G in less)
Type "G"
Sleep 3s

# Quit the pager
Type "q"
Sleep 2s

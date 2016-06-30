for file in *.COMPLETED; do
    mv -- "$file" "${file%%.COMPLETED}"
done
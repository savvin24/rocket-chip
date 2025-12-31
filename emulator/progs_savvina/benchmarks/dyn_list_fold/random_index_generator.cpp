#include <iostream> // For console output (e.g., error messages)
#include <fstream>  // For file operations (ofstream)
#include <cstdlib>  // For rand() and srand()
#include <ctime>    // For time() to seed srand()

int main() {
    // Define the number of random indices to generate
    const int num_indices = 6000;

    // Define the range for the random indices (inclusive)
    const int min_index = 0;
    const int max_index = 2999;

    // Create an output file stream
    // The file will be named "random_indices.txt"
    std::ofstream output_file("random_indices.txt");

    // Check if the file was opened successfully
    if (!output_file.is_open()) {
        std::cerr << "Error: Could not open the file 'random_indices.txt' for writing." << std::endl;
        return 1; // Indicate an error
    }

    // Seed the random number generator using the current time.
    // This ensures a different sequence of random numbers each time the program runs.
    std::srand(static_cast<unsigned int>(std::time(nullptr)));

    // Generate and write the random indices to the file
    for (int i = 0; i < num_indices; ++i) {
        // Generate a random index using rand() and the modulo operator.
        // The formula rand() % (max - min + 1) + min generates numbers
        // uniformly within the range [min, max] inclusive.
        int random_index = min_index + (rand() % (max_index - min_index + 1));
        // Write the index to the file, followed by a newline character
        output_file << random_index << std::endl;
    }

    // Close the output file
    // It's good practice to explicitly close files, though it will be closed automatically
    // when output_file goes out of scope.
    output_file.close();

    std::cout << "Successfully generated " << num_indices << " random indices "
              << "between " << min_index << " and " << max_index << " (inclusive) "
              << "and saved them to 'random_indices.txt'." << std::endl;

    return 0; // Indicate successful execution
}